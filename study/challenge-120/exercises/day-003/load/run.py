#!/usr/bin/env python3
"""Start disposable MySQL + test-only HTTP server, run local k6, and clean up."""
import json, os, signal, subprocess, time, urllib.request, socket, argparse
from pathlib import Path

exercise=Path(__file__).resolve().parents[1]
parser=argparse.ArgumentParser()
parser.add_argument('--nginx',action='store_true',help='Proxy k6 through an isolated local Nginx with 64 idle upstream connections')
args=parser.parse_args()
project=exercise.parents[1]
out=exercise/'build/load'
ready=exercise/'build/load-server.json'
if ready.exists(): raise SystemExit(f'Existing fixture marker: {ready}; inspect before starting another server')
if out.exists():
    archive=out.with_name(f'load-{time.time_ns()}')
    out.rename(archive)
    print(f'Previous results preserved: {archive}',flush=True)
out.mkdir(parents=True,exist_ok=True)
server=None; k6=None; server_pid=None; samples=[]; nginx=None; nginx_log=None
with (out/'server.log').open('w') as log:
    try:
        server=subprocess.Popen([str(project/'gradlew'),'-p',str(project),':day-003:loadServer','--no-daemon','--console=plain'],stdout=log,stderr=subprocess.STDOUT,start_new_session=True)
        deadline=time.monotonic()+180
        while not ready.exists():
            if server.poll() is not None: raise RuntimeError(f'Fixture exited; see {out / "server.log"}')
            if time.monotonic()>deadline: raise TimeoutError('Fixture startup')
            time.sleep(.5)
        info=json.loads(ready.read_text()); server_pid=info['pid']
        print('Fixture:',json.dumps(info),flush=True)
        target=info['url']
        if args.nginx:
            with socket.socket() as port_probe:
                port_probe.bind(('127.0.0.1',0)); proxy_port=port_probe.getsockname()[1]
            config=out/'nginx.conf'
            config.write_text(f'''worker_processes 1;
pid {out}/nginx.pid;
error_log {out}/nginx-error.log info;
events {{ worker_connections 4096; }}
http {{
    log_format timing '$status upstream=$upstream_status connect=$upstream_connect_time response=$upstream_response_time';
    access_log {out}/nginx-access.log timing;
    upstream application {{
        server {info['url'].removeprefix('http://')};
        keepalive 64;
        keepalive_timeout 10s;
    }}
    server {{
        listen 127.0.0.1:{proxy_port};
        location / {{
            proxy_pass http://application;
            proxy_http_version 1.1;
            proxy_set_header Connection "";
            proxy_next_upstream off;
        }}
    }}
}}
''')
            subprocess.run(['nginx','-e',str(out/'nginx-error.log'),'-t','-p',str(out)+'/', '-c',str(config)],check=True)
            nginx_log=(out/'nginx-process.log').open('w')
            nginx=subprocess.Popen(['nginx','-e',str(out/'nginx-error.log'),'-p',str(out)+'/', '-c',str(config),'-g','daemon off;'],stdout=nginx_log,stderr=subprocess.STDOUT)
            target=f'http://127.0.0.1:{proxy_port}'
            for attempt in range(50):
                if nginx.poll() is not None: raise RuntimeError('Nginx exited; inspect nginx-process.log')
                try:
                    with urllib.request.urlopen(target+'/state',timeout=1) as response: json.load(response)
                    break
                except OSError: time.sleep(.1)
            else: raise TimeoutError('Nginx startup')
            print('Nginx target:',target,flush=True)
        env={**os.environ,'BASE_URL':target,'SUMMARY_PATH':str(out/'k6-summary.json'),
             'K6_WEB_DASHBOARD':'true','K6_WEB_DASHBOARD_PORT':os.environ.get('K6_WEB_DASHBOARD_PORT','-1'),
             'K6_WEB_DASHBOARD_PERIOD':'2s','K6_WEB_DASHBOARD_EXPORT':str(out/'k6-report.html')}
        with (out/'k6.log').open('w') as klog:
            k6=subprocess.Popen(['k6','run',str(exercise/'load/mixed-reservations.js')],cwd=exercise,env=env,stdout=klog,stderr=subprocess.STDOUT)
            started=time.monotonic()
            while k6.poll() is None:
                sample={'seconds':round(time.monotonic()-started,2)}
                try:
                    with urllib.request.urlopen(info['url']+'/state',timeout=5) as response: sample.update(json.load(response))
                    fd=subprocess.run(['lsof','-nP','-a','-p',str(k6.pid),'-Ff'],text=True,capture_output=True,timeout=4)
                    sample['k6Fds']=sum(1 for x in fd.stdout.splitlines() if x.startswith('f') and x[1:].isdigit())
                except Exception as failure: sample['samplingError']=str(failure)
                samples.append(sample)
                time.sleep(2)
        with urllib.request.urlopen(info['url']+'/state',timeout=5) as response: final=json.load(response)
        summary=json.loads((out/'k6-summary.json').read_text())
        result={'fixture':info,'target':target,'nginx':args.nginx,'k6ExitCode':k6.returncode,'final':final,'samples':samples,
                'observedMaxServerFds':max(s.get('openFds',0) for s in samples),
                'observedMaxK6Fds':max(s.get('k6Fds',0) for s in samples),
                'observedMaxMySqlConnections':max(s.get('mysqlConnections',0) for s in samples),
                'sampledInvariantViolations':sum(1 for s in samples if 'remaining' in s and (s['remaining']+s['reservations']!=1 or s['remaining']<0 or s['reservations']>1)),
                'metrics':{k:v['values'] for k,v in summary['metrics'].items() if k in ['http_reqs','http_req_failed','http_req_duration','checks','vus_max','reservations_created','reservations_sold_out']}}
        (out/'result.json').write_text(json.dumps(result,indent=2))
        print(json.dumps({k:v for k,v in result.items() if k!='samples'},indent=2),flush=True)
        if k6.returncode or result['sampledInvariantViolations']: raise SystemExit(1)
    finally:
        if k6 is not None and k6.poll() is None: k6.terminate(); k6.wait(timeout=10)
        if nginx is not None and nginx.poll() is None: nginx.terminate(); nginx.wait(timeout=10)
        if nginx_log is not None: nginx_log.close()
        if server_pid is not None:
            try: os.kill(server_pid,signal.SIGTERM)
            except ProcessLookupError: pass
        if server is not None:
            try: server.wait(timeout=15)
            except subprocess.TimeoutExpired:
                os.killpg(server.pid,signal.SIGTERM); server.wait(timeout=10)

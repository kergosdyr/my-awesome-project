import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const directory = path.dirname(fileURLToPath(import.meta.url));
const questionsMarkdown = fs.readFileSync(path.join(directory, 'questions.md'), 'utf8');
const answersMarkdown = fs.readFileSync(path.join(directory, 'model-answers.md'), 'utf8');
const idPrefix = '(?:OS|JAVA|DB|NET|REDIS|KAFKA|JD)';

const questions = [];
const questionIds = new Set();
let domain = '';
let topic = '';

for (const line of questionsMarkdown.split('\n')) {
  const domainMatch = line.match(/^# \d+\. (.+)$/);
  if (domainMatch) {
    domain = domainMatch[1];
    continue;
  }
  const topicMatch = line.match(/^## \d+\.\d+ (.+)$/);
  if (topicMatch) {
    topic = topicMatch[1];
    continue;
  }
  const questionMatch = line.match(new RegExp('^- \\*\\*(' + idPrefix + '-\\d+) (★+)\\*\\* (.+)$'));
  if (questionMatch) {
    if (questionIds.has(questionMatch[1])) throw new Error(`Duplicate question ID: ${questionMatch[1]}`);
    questionIds.add(questionMatch[1]);
    questions.push({
      id: questionMatch[1],
      priority: questionMatch[2].length,
      domain,
      topic,
      question: questionMatch[3],
    });
  }
}

const answers = new Map();
let currentAnswer = null;

function flushAnswer() {
  if (!currentAnswer) return;
  const body = currentAnswer.lines.join('\n').trim();
  const answer = body.match(/\*\*면접 답변:\*\*\s*([^\n]+)/)?.[1] ?? '';
  const easy = body.match(/\*\*쉽게 말하면:\*\*\s*([^\n]+)/)?.[1] ?? '';
  const caution = body.match(/\*\*주의:\*\*\s*([^\n]+)/)?.[1] ?? '';
  if (answers.has(currentAnswer.id)) throw new Error(`Duplicate answer ID: ${currentAnswer.id}`);
  answers.set(currentAnswer.id, { answer, easy, caution });
}

for (const line of answersMarkdown.split('\n')) {
  const headingMatch = line.match(new RegExp('^### (' + idPrefix + '-\\d+)\\.\\s*(.+)$'));
  if (headingMatch) {
    flushAnswer();
    currentAnswer = { id: headingMatch[1], title: headingMatch[2], lines: [] };
  } else if (currentAnswer) {
    if (/^#{1,3} /.test(line) || line === '---') {
      flushAnswer();
      currentAnswer = null;
    } else {
      currentAnswer.lines.push(line);
    }
  }
}
flushAnswer();

const data = questions.map((question) => ({ ...question, ...(answers.get(question.id) ?? {}) }));
const importantCount = data.filter((item) => item.priority === 3).length;
const missing = data.filter((item) => !item.answer);
if (missing.length) {
  throw new Error(`Missing answers: ${missing.map((item) => item.id).join(', ')}`);
}
const orphanAnswers = [...answers.keys()].filter((id) => !questionIds.has(id));
if (orphanAnswers.length) {
  throw new Error(`Answers without questions: ${orphanAnswers.join(', ')}`);
}
const declaredTotal = Number(questionsMarkdown.match(/\*\*총 (\d+)문항\*\*/)?.[1]);
if (declaredTotal !== data.length) {
  throw new Error(`Declared total ${declaredTotal} does not match parsed total ${data.length}`);
}
const declaredDomainCounts = new Map(
  [...questionsMarkdown.matchAll(/^- (.+) (\d+)문항$/gm)].map((match) => [match[1], Number(match[2])]),
);
const actualDomainCounts = data.reduce((counts, item) => {
  counts.set(item.domain, (counts.get(item.domain) ?? 0) + 1);
  return counts;
}, new Map());
for (const [name, count] of actualDomainCounts) {
  if (declaredDomainCounts.get(name) !== count) {
    throw new Error(`Declared domain count for ${name} does not match parsed count ${count}`);
  }
}

const safeData = JSON.stringify(data).replaceAll('<', '\\u003c');

const html = `<!doctype html>
<html lang="ko">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <meta name="description" content="Java, 데이터베이스, 네트워크, 운영체제, Redis, Kafka, 핵클 JD 면접 ${data.length}문항 학습 가이드">
  <title>CS 면접 한 장씩</title>
  <style>
    :root {
      color-scheme: light;
      --paper: #f4f1e8;
      --paper-2: #fffdf6;
      --ink: #17231c;
      --muted: #68716a;
      --line: #d8d4c7;
      --green: #0b6b4b;
      --green-soft: #dcece3;
      --orange: #f06c35;
      --yellow: #f5c84c;
      --blue: #3178c6;
      --red: #c94040;
      --shadow: 0 18px 48px rgba(44, 53, 47, .10);
      --radius: 18px;
      --sidebar: 294px;
    }

    * { box-sizing: border-box; }
    html { scroll-behavior: smooth; }
    body {
      margin: 0;
      background:
        radial-gradient(circle at 80% 0%, rgba(245, 200, 76, .13), transparent 27rem),
        var(--paper);
      color: var(--ink);
      font-family: Pretendard, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
      line-height: 1.65;
    }
    button, input { font: inherit; }
    button { color: inherit; }
    :focus-visible { outline: 3px solid rgba(240, 108, 53, .42); outline-offset: 3px; }
    .skip { position: fixed; left: 12px; top: -80px; z-index: 99; background: var(--ink); color: white; padding: 10px 14px; }
    .skip:focus { top: 12px; }

    .shell { min-height: 100vh; }
    .sidebar {
      position: fixed;
      inset: 0 auto 0 0;
      width: var(--sidebar);
      overflow-y: auto;
      padding: 26px 22px;
      background: rgba(255, 253, 246, .91);
      border-right: 1px solid var(--line);
      backdrop-filter: blur(16px);
      z-index: 20;
    }
    .brand { display: flex; align-items: center; gap: 12px; margin-bottom: 26px; }
    .brand-mark {
      width: 44px; height: 44px; display: grid; place-items: center;
      border: 2px solid var(--ink); border-radius: 12px;
      background: var(--yellow); font-weight: 900; box-shadow: 4px 4px 0 var(--ink);
    }
    .brand strong { display: block; font-size: 18px; letter-spacing: -.04em; }
    .brand small { color: var(--muted); }
    .side-label { margin: 20px 0 8px; color: var(--muted); font-size: 12px; font-weight: 800; letter-spacing: .11em; }
    .view-tabs, .domain-list { display: grid; gap: 7px; }
    .nav-button, .domain-button {
      width: 100%; border: 0; background: transparent; text-align: left;
      padding: 11px 12px; border-radius: 11px; cursor: pointer;
    }
    .nav-button:hover, .domain-button:hover { background: var(--green-soft); }
    .nav-button.active, .domain-button.active { background: var(--ink); color: white; }
    .domain-button { display: flex; justify-content: space-between; align-items: center; }
    .domain-button span:last-child { font-size: 12px; opacity: .72; }
    .progress-box { margin-top: 24px; padding-top: 18px; border-top: 1px solid var(--line); }
    .progress-row { display: flex; justify-content: space-between; font-size: 13px; }
    .progress-track { height: 8px; background: #dfddd4; border-radius: 99px; margin-top: 9px; overflow: hidden; }
    .progress-fill { width: 0; height: 100%; background: var(--green); transition: width .3s ease; }
    .main { margin-left: var(--sidebar); padding: 42px clamp(24px, 5vw, 72px) 90px; }
    .content { max-width: 1040px; margin: 0 auto; }

    .hero { display: grid; grid-template-columns: 1.35fr .65fr; gap: 28px; align-items: end; margin-bottom: 34px; }
    .eyebrow { color: var(--green); font-size: 13px; font-weight: 900; letter-spacing: .12em; }
    h1 { margin: 8px 0 12px; max-width: 760px; font-size: clamp(38px, 6vw, 72px); line-height: .98; letter-spacing: -.075em; }
    .hero p { max-width: 680px; margin: 0; color: var(--muted); font-size: 17px; }
    .hero-note { border-left: 3px solid var(--orange); padding-left: 15px; color: var(--muted); font-size: 14px; }
    .hero-note strong { display: block; color: var(--ink); font-size: 22px; }

    .controls {
      position: sticky; top: 0; z-index: 12;
      display: grid; grid-template-columns: 1fr auto; gap: 12px;
      padding: 14px 0; background: rgba(244, 241, 232, .88); backdrop-filter: blur(12px);
    }
    .search-wrap { position: relative; }
    .search-wrap svg { position: absolute; left: 16px; top: 50%; transform: translateY(-50%); color: var(--muted); }
    .search {
      width: 100%; min-height: 48px; padding: 0 16px 0 46px;
      border: 1px solid var(--line); border-radius: 12px; background: var(--paper-2); color: var(--ink);
    }
    .priority-filter { display: flex; align-items: center; gap: 6px; padding: 5px; border: 1px solid var(--line); border-radius: 12px; background: var(--paper-2); }
    .priority-filter button { border: 0; background: transparent; padding: 7px 10px; border-radius: 8px; cursor: pointer; }
    .priority-filter button.active { background: var(--green-soft); color: var(--green); font-weight: 800; }
    .result-meta { display: flex; justify-content: space-between; align-items: center; margin: 14px 0 18px; color: var(--muted); font-size: 14px; }
    .expand-button { border: 0; background: transparent; color: var(--green); cursor: pointer; font-weight: 800; }
    .topic-heading { margin: 32px 0 10px; font-size: 20px; letter-spacing: -.03em; }
    .topic-heading span { color: var(--muted); font-size: 13px; font-weight: 500; margin-left: 7px; }

    .question-card { margin-bottom: 12px; border: 1px solid var(--line); border-radius: var(--radius); background: var(--paper-2); box-shadow: 0 3px 0 rgba(23,35,28,.03); overflow: hidden; }
    .question-card[open] { border-color: rgba(11,107,75,.45); box-shadow: var(--shadow); }
    .question-card summary { list-style: none; cursor: pointer; display: grid; grid-template-columns: auto 1fr auto; gap: 14px; align-items: start; padding: 19px 20px; }
    .question-card summary::-webkit-details-marker { display: none; }
    .qid { min-width: 70px; color: var(--green); font-size: 13px; font-weight: 900; letter-spacing: .03em; }
    .question { font-weight: 760; line-height: 1.45; letter-spacing: -.018em; }
    .stars { color: #ca8e00; white-space: nowrap; font-size: 13px; }
    .answer-panel { padding: 0 20px 22px 104px; }
    .answer-block { border-top: 1px solid var(--line); padding-top: 18px; }
    .answer-label { display: block; margin-bottom: 6px; color: var(--orange); font-size: 12px; font-weight: 900; letter-spacing: .11em; }
    .one-line { margin: 0 0 16px; font-size: 18px; font-weight: 760; line-height: 1.55; }
    .steps { margin: 0; padding: 0; list-style: none; counter-reset: explain; display: grid; gap: 10px; }
    .steps li { position: relative; padding-left: 34px; color: #3d4941; }
    .steps li::before { counter-increment: explain; content: counter(explain); position: absolute; left: 0; top: 1px; width: 23px; height: 23px; display: grid; place-items: center; border-radius: 50%; background: var(--green-soft); color: var(--green); font-size: 12px; font-weight: 900; }
    .term-list { display: grid; grid-template-columns: repeat(auto-fit, minmax(190px, 1fr)); gap: 8px; margin: 10px 0 0; }
    .term { padding: 10px 12px; border: 1px solid var(--line); background: white; font-size: 13px; color: #3d4941; }
    .term b { display: block; color: var(--green); margin-bottom: 2px; }
    .easy-note, .caution-note { margin-top: 16px; padding: 13px 15px; border-left: 3px solid var(--yellow); background: rgba(245,200,76,.12); }
    .caution-note { border-left-color: var(--red); background: rgba(201,64,64,.07); }
    .card-actions { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 18px; }
    .status-button, .diagram-button, .practice-action, .primary-action {
      border: 1px solid var(--line); background: white; border-radius: 10px; padding: 8px 11px; cursor: pointer; font-size: 13px; font-weight: 750;
    }
    .status-button[data-active="true"] { background: var(--green); border-color: var(--green); color: white; }
    .status-button.review[data-active="true"] { background: var(--orange); border-color: var(--orange); }
    .diagram-button { color: var(--blue); }
    .diagram { display: none; margin-top: 18px; padding: 18px; border: 1px dashed #b8b3a4; background: #f8f5ea; overflow-x: auto; }
    .diagram.visible { display: block; animation: reveal .25s ease; }
    @keyframes reveal { from { opacity: 0; transform: translateY(-4px); } }
    .flow { min-width: 620px; display: flex; align-items: center; justify-content: center; gap: 8px; padding: 20px 4px; }
    .flow-node { position: relative; min-width: 106px; padding: 12px 10px; border: 2px solid var(--ink); background: white; text-align: center; font-size: 13px; font-weight: 850; box-shadow: 3px 3px 0 var(--ink); }
    .flow-node small { display: block; color: var(--muted); font-weight: 500; margin-top: 2px; }
    .flow-arrow { position: relative; width: 38px; height: 2px; background: var(--ink); }
    .flow-arrow::after { content: ''; position: absolute; right: -1px; top: -4px; border-left: 7px solid var(--ink); border-top: 5px solid transparent; border-bottom: 5px solid transparent; }
    .packet { position: absolute; z-index: 2; top: -5px; left: 0; width: 11px; height: 11px; border-radius: 50%; background: var(--orange); animation: travel 1.4s linear infinite; }
    @keyframes travel { from { transform: translateX(-2px); } to { transform: translateX(35px); } }
    .memory-map { display: grid; grid-template-columns: 1fr auto 1fr; gap: 22px; min-width: 600px; align-items: center; padding: 10px; }
    .memory-column { display: grid; grid-template-columns: repeat(4,1fr); gap: 6px; }
    .page { min-height: 54px; display: grid; place-items: center; border: 1px solid var(--ink); background: white; font-size: 12px; font-weight: 800; }
    .page.active { animation: pagePulse 2s ease infinite; background: var(--green-soft); }
    @keyframes pagePulse { 50% { background: #ffdf83; transform: translateY(-3px); } }
    .cycle { min-width: 560px; height: 230px; position: relative; margin: 0 auto; }
    .cycle .flow-node { position: absolute; width: 120px; }
    .cycle .n1 { left: 50%; top: 0; transform: translateX(-50%); }
    .cycle .n2 { right: 25px; top: 120px; }
    .cycle .n3 { left: 25px; top: 120px; }
    .cycle svg { position: absolute; inset: 0; width: 100%; height: 100%; }
    .cycle path { fill: none; stroke: var(--orange); stroke-width: 3; stroke-dasharray: 9 7; animation: dash 1s linear infinite; }
    @keyframes dash { to { stroke-dashoffset: -16; } }
    .lanes { min-width: 620px; position: relative; padding: 8px 28px 18px; }
    .lane-head { display: flex; justify-content: space-between; font-weight: 900; }
    .message { position: relative; height: 38px; margin: 5px 70px; }
    .message::before { content: ''; position: absolute; left: 0; right: 0; top: 18px; height: 2px; background: var(--ink); transform: rotate(var(--angle, 0deg)); }
    .message::after { content: attr(data-label); position: absolute; left: 50%; top: 0; padding: 0 7px; transform: translateX(-50%); background: #f8f5ea; color: var(--green); font-size: 12px; font-weight: 900; }
    .message.reverse::before { transform: rotate(calc(var(--angle, 0deg) * -1)); }
    .tree { min-width: 620px; text-align: center; }
    .tree-level { display: flex; justify-content: center; gap: 12px; margin: 18px 0; }
    .tree-node { display: flex; border: 2px solid var(--ink); background: white; box-shadow: 3px 3px 0 var(--ink); }
    .tree-node span { padding: 8px 12px; border-right: 1px solid var(--line); font-weight: 800; }
    .tree-node span:last-child { border: 0; }
    .tree-node .hit { background: var(--yellow); animation: pagePulse 1.7s ease infinite; }
    .legend { margin-top: 12px; color: var(--muted); text-align: center; font-size: 12px; }

    .practice { display: none; }
    .practice.active, .list.active { display: block; }
    .list:not(.active) { display: none; }
    .practice-stage { min-height: 580px; display: grid; place-items: center; padding: 20px 0; }
    .flashcard { width: min(780px, 100%); padding: clamp(24px,5vw,52px); border: 2px solid var(--ink); background: var(--paper-2); box-shadow: 10px 10px 0 var(--ink); }
    .flashcard-top { display: flex; justify-content: space-between; gap: 16px; color: var(--muted); font-size: 13px; }
    .flash-question { margin: 42px 0; font-size: clamp(25px,4vw,42px); line-height: 1.28; letter-spacing: -.045em; }
    .answer-reveal { display: none; border-top: 1px solid var(--line); padding-top: 24px; }
    .answer-reveal.visible { display: block; animation: reveal .25s ease; }
    .primary-action { background: var(--green); color: white; border-color: var(--green); padding: 11px 16px; }
    .practice-actions { display: flex; justify-content: space-between; gap: 10px; align-items: center; margin-top: 25px; }
    .practice-actions .right { display: flex; gap: 8px; }
    .empty { padding: 70px 20px; text-align: center; color: var(--muted); }
    .mobile-top { display: none; }

    @media (max-width: 860px) {
      .sidebar { transform: translateX(-100%); transition: transform .25s ease; box-shadow: var(--shadow); }
      .sidebar.open { transform: translateX(0); }
      .main { margin-left: 0; padding: 24px 18px 70px; }
      .mobile-top { display: flex; position: sticky; top: 0; z-index: 18; justify-content: space-between; align-items: center; padding: 10px 14px; margin: -24px -18px 22px; background: rgba(244,241,232,.92); border-bottom: 1px solid var(--line); backdrop-filter: blur(12px); }
      .mobile-top button { border: 1px solid var(--line); background: var(--paper-2); border-radius: 9px; padding: 7px 10px; }
      .hero { grid-template-columns: 1fr; }
      .hero-note { display: none; }
      .controls { grid-template-columns: 1fr; }
      .priority-filter { overflow-x: auto; }
      .question-card summary { grid-template-columns: 1fr auto; }
      .qid { grid-column: 1; }
      .question { grid-column: 1 / -1; }
      .stars { grid-column: 2; grid-row: 1; }
      .answer-panel { padding: 0 16px 20px; }
      .flow, .memory-map, .lanes, .tree { min-width: 570px; }
    }

    @media (prefers-reduced-motion: reduce) {
      *, *::before, *::after { scroll-behavior: auto !important; animation: none !important; transition: none !important; }
    }
  </style>
</head>
<body>
  <a class="skip" href="#main">본문으로 건너뛰기</a>
  <div class="shell">
    <aside class="sidebar" id="sidebar" aria-label="학습 메뉴">
      <div class="brand">
        <div class="brand-mark">CS</div>
        <div><strong>면접 한 장씩</strong><small>${data.length} questions</small></div>
      </div>
      <div class="side-label">학습 방식</div>
      <div class="view-tabs">
        <button class="nav-button active" data-view="list">전체 해설 보기</button>
        <button class="nav-button" data-view="practice">한 문제씩 연습</button>
      </div>
      <div class="side-label">과목</div>
      <div class="domain-list" id="domain-list"></div>
      <div class="progress-box">
        <div class="progress-row"><strong>내 학습 진도</strong><span id="progress-text">0 / ${data.length}</span></div>
        <div class="progress-track"><div class="progress-fill" id="progress-fill"></div></div>
      </div>
    </aside>

    <main class="main" id="main">
      <div class="mobile-top"><strong>CS 면접 한 장씩</strong><button id="menu-button" aria-expanded="false">메뉴</button></div>
      <div class="content">
        <header class="hero">
          <div>
            <div class="eyebrow">CHANNELTALK INTERVIEW CURRICULUM · REBUILT</div>
            <h1>외우는 CS 말고,<br>설명하는 CS.</h1>
            <p>질문을 먼저 보고, 결론부터 말하고, 그림으로 원리를 연결하세요. 영어 단어보다 “왜 그렇게 동작하는지”를 자기 말로 설명하는 데 초점을 맞췄습니다.</p>
          </div>
          <div class="hero-note"><strong>${importantCount}개</strong>★★★ 질문부터 시작하면 첫 회독이 끝납니다.</div>
        </header>

        <section class="list active" id="list-view" aria-label="전체 질문 해설">
          <div class="controls">
            <label class="search-wrap">
              <svg width="19" height="19" viewBox="0 0 24 24" aria-hidden="true"><circle cx="11" cy="11" r="7" fill="none" stroke="currentColor" stroke-width="2"/><path d="m16 16 5 5" stroke="currentColor" stroke-width="2"/></svg>
              <input class="search" id="search" type="search" placeholder="예: 가상 메모리, GC, 트랜잭션, TLS" aria-label="질문 검색">
            </label>
            <div class="priority-filter" aria-label="중요도 필터">
              <button class="active" data-priority="0">전체</button>
              <button data-priority="3">★★★</button>
              <button data-priority="2">★★</button>
              <button data-priority="1">★</button>
            </div>
          </div>
          <div class="result-meta"><span id="result-count"></span><button class="expand-button" id="expand-all">현재 결과 모두 펼치기</button></div>
          <div id="questions"></div>
        </section>

        <section class="practice" id="practice-view" aria-label="한 문제씩 연습">
          <div class="practice-stage" id="practice-stage"></div>
        </section>
      </div>
    </main>
  </div>

  <script>
    const DATA = ${safeData};
    const GLOSSARY = {
      'JVM': '자바 바이트코드를 실제 컴퓨터에서 실행하는 가상 실행 환경',
      'JIT': '자주 실행되는 코드를 실행 중 기계어로 바꾸는 컴파일러',
      'GC': '더 이상 쓰지 않는 객체의 메모리를 자동으로 회수하는 기능',
      'Heap': '실행 중 만들어진 객체가 주로 저장되는 공유 메모리 영역',
      'Stack': '함수 호출과 지역 변수가 실행 흐름별로 쌓이는 메모리',
      'PCB': '운영체제가 프로세스의 현재 상태를 기록하는 관리 카드',
      'TLB': '최근 주소 변환 결과를 기억하는 CPU 안의 빠른 캐시',
      'MMU': '가상 주소를 물리 주소로 바꾸고 접근 권한을 검사하는 장치',
      'Page Fault': '필요한 메모리 페이지가 RAM에 없어 운영체제 도움이 필요한 사건',
      'Context Switch': 'CPU가 실행 대상을 바꾸며 작업 상태를 저장하고 복원하는 과정',
      'Race Condition': '동시 실행 순서에 따라 공유 데이터 결과가 달라지는 문제',
      'Deadlock': '서로의 자원을 기다리느라 아무도 진행하지 못하는 상태',
      'Blocking': '결과가 준비될 때까지 현재 실행 흐름이 기다리는 방식',
      'Non-Blocking': '결과가 없어도 제어권을 바로 돌려주는 방식',
      'Event Loop': '준비된 짧은 작업을 차례로 꺼내 처리하는 반복 실행 구조',
      'Transaction': '여러 데이터 변경을 하나의 성공 또는 실패 단위로 묶는 것',
      'MVCC': '행의 여러 버전을 보관해 읽기와 쓰기의 충돌을 줄이는 방식',
      'Lock': '동시에 같은 자원을 바꾸지 못하도록 접근 순서를 제어하는 장치',
      'Index': '원하는 행을 빠르게 찾도록 별도로 정렬해 둔 찾아보기 구조',
      'B+Tree': '디스크 페이지를 적게 읽으면서 정렬·범위 검색을 지원하는 균형 트리',
      'Connection Pool': '미리 만든 DB 연결을 빌리고 반환하며 재사용하는 저장소',
      'TCP': '순서와 전달을 확인하고 유실 데이터를 다시 보내는 전송 규약',
      'UDP': '연결 설정과 전달 보장을 줄여 가볍게 Datagram을 보내는 규약',
      'DNS': '도메인 이름을 IP 주소 같은 정보로 바꾸는 분산 전화번호부',
      'HTTP': '웹 클라이언트와 서버가 요청과 응답을 주고받는 규약',
      'TLS': '통신 상대를 확인하고 전송 데이터를 암호화하는 보안 규약',
      'REST': '자원과 표준 HTTP 의미를 중심으로 API를 구성하는 설계 방식',
      'CORS': '브라우저가 다른 출처의 응답을 읽도록 서버가 허용하는 규약',
      'JWT': '내용과 서명을 세 부분에 담는 토큰 형식',
      'OAuth': '비밀번호를 넘기지 않고 제한된 접근 권한을 위임하는 규약',
      'WebSocket': '한 연결에서 양쪽이 자유롭게 메시지를 보내는 통신 방식',
      'SSE': '서버가 HTTP 연결을 통해 클라이언트로 이벤트를 계속 보내는 방식',
      'TTL': '데이터가 자동으로 만료될 때까지 남은 시간',
      'Eviction': '메모리가 부족할 때 정책에 따라 데이터를 내보내는 것',
      'Sentinel': 'Redis Primary를 감시하고 장애 시 승격을 조정하는 구성 요소',
      'Cache Stampede': '인기 캐시 만료 순간 요청이 원본 DB로 한꺼번에 몰리는 현상',
      'Topic': 'Kafka 이벤트를 업무 종류별로 묶는 논리적 이름',
      'Partition': 'Kafka Topic을 나눈 순서 있는 실제 로그 조각',
      'Offset': 'Kafka Partition 안에서 이벤트의 위치를 나타내는 번호',
      'Consumer Group': 'Partition을 나눠 읽는 Consumer들의 한 논리적 구독자',
      'ISR': 'Kafka Leader를 충분히 따라온 동기화 Replica 집합',
      'DLQ': '반복 처리할 수 없는 실패 메시지를 조사하도록 격리하는 큐',
      'Idempotency': '같은 요청을 여러 번 처리해도 최종 결과가 한 번과 같게 하는 성질',
      'Outbox': 'DB 변경과 발행할 이벤트를 같은 DB Transaction에 기록하는 패턴',
      'Backpressure': '소비자가 감당할 수 있는 양만 생산자에게 요청하는 흐름 제어',
      'SDK': '다른 개발자가 제품 기능을 코드에서 쉽게 쓰도록 제공하는 라이브러리',
      'Snapshot': '한 시점의 완성된 상태를 통째로 고정한 사본',
      'SLO': '사용자에게 제공하겠다고 내부적으로 정한 신뢰성 목표',
      'Kubernetes': 'Container 실행·배포·복구를 여러 서버에서 조정하는 플랫폼',
      'Readiness': '현재 새 트래픽을 받을 준비가 됐는지 나타내는 상태',
      'Liveness': '프로세스를 재시작해야 할 만큼 멈췄는지 나타내는 상태',
      'Coroutine': '대기할 때 Thread를 점유하지 않고 중단·재개할 수 있는 실행 단위',
      'Reactive Streams': '비동기 데이터 흐름과 Backpressure 계약을 정의한 표준',
      'ORM': '객체와 관계형 DB 행을 서로 연결해 주는 계층',
      'N+1': '목록 한 번 뒤 항목 수만큼 연관 Query가 반복되는 문제',
      'Trace': '한 요청이 여러 서비스와 작업을 지나간 전체 경로 기록',
      'Tenant': '하나의 SaaS를 독립적으로 사용하는 고객 조직',
      'A/B': '사용자를 여러 그룹으로 나눠 변화의 효과를 비교하는 실험',
      'Feature Flag': '배포한 기능의 노출 여부를 설정으로 제어하는 스위치',
      'RDBMS': '관계와 제약 조건을 Table로 관리하고 SQL로 조회하는 데이터베이스',
      'MongoDB': '관련 데이터를 JSON과 비슷한 Document 단위로 저장하는 데이터베이스',
      'Elasticsearch': '역색인을 사용해 검색과 집계를 빠르게 수행하는 분산 검색 엔진',
      'SQS': 'AWS가 운영해 주는 작업 메시지 Queue 서비스',
      'Kinesis': 'AWS에서 Shard 단위로 Event Stream을 수집·처리하는 서비스',
      'Docker': '애플리케이션과 실행 환경을 Image로 묶어 격리 실행하는 도구',
      'AWS': '컴퓨팅·저장·네트워크 서비스를 필요할 때 빌려 쓰는 Cloud 플랫폼',
      'MSA': '업무 경계를 기준으로 독립 배포 가능한 작은 서비스로 나누는 구조'
    };
    const state = {
      domain: '전체',
      priority: 0,
      query: '',
      view: 'list',
      practiceIndex: 0,
      practiceAnswerVisible: false,
      progress: JSON.parse(localStorage.getItem('cs-interview-progress') || '{}')
    };

    const domainNames = ['전체', ...new Set(DATA.map((item) => item.domain))];
    const domainList = document.getElementById('domain-list');
    const questionsRoot = document.getElementById('questions');
    const resultCount = document.getElementById('result-count');
    const listView = document.getElementById('list-view');
    const practiceView = document.getElementById('practice-view');
    const practiceStage = document.getElementById('practice-stage');

    function escapeHtml(value = '') {
      return value.replace(/[&<>'"]/g, (character) => ({ '&':'&amp;', '<':'&lt;', '>':'&gt;', "'":'&#39;', '"':'&quot;' })[character]);
    }

    function splitSentences(text) {
      return text.split(/(?<=[가-힣][.!?])\\s+/u).map((value) => value.trim()).filter(Boolean);
    }

    function glossaryFor(item) {
      const haystack = [item.question, item.answer].join(' ').toLocaleLowerCase('ko');
      const hasBoundedTerm = (term) => {
        const target = term.toLocaleLowerCase('ko');
        let index = haystack.indexOf(target);
        while (index >= 0) {
          const before = haystack[index - 1] ?? '';
          const after = haystack[index + target.length] ?? '';
          if (!/[A-Za-z0-9]/.test(before) && !/[A-Za-z0-9]/.test(after)) return true;
          index = haystack.indexOf(target, index + 1);
        }
        return false;
      };
      return Object.entries(GLOSSARY)
        .sort(([left], [right]) => right.length - left.length)
        .filter(([term]) => hasBoundedTerm(term))
        .slice(0, 5);
    }

    function filteredData() {
      const query = state.query.trim().toLocaleLowerCase('ko');
      return DATA.filter((item) => {
        const domainMatches = state.domain === '전체' || item.domain === state.domain;
        const priorityMatches = state.priority === 0 || item.priority === state.priority;
        const haystack = [item.id, item.domain, item.topic, item.question, item.answer, item.easy].join(' ').toLocaleLowerCase('ko');
        return domainMatches && priorityMatches && (!query || haystack.includes(query));
      });
    }

    function renderDomains() {
      domainList.innerHTML = domainNames.map((name) => {
        const count = name === '전체' ? DATA.length : DATA.filter((item) => item.domain === name).length;
        return '<button class="domain-button ' + (state.domain === name ? 'active' : '') + '" data-domain="' + escapeHtml(name) + '"><span>' + escapeHtml(name) + '</span><span>' + count + '</span></button>';
      }).join('');
    }

    function statusButtons(item) {
      const status = state.progress[item.id] || '';
      return '<button class="status-button review" data-status="review" data-id="' + item.id + '" data-active="' + (status === 'review') + '">↺ 다시 보기</button>' +
        '<button class="status-button" data-status="done" data-id="' + item.id + '" data-active="' + (status === 'done') + '">✓ 이해했어요</button>';
    }

    function diagramFor(id) {
      if (id === 'OS-18') return '<div class="memory-map"><div><b>가상 페이지</b><div class="memory-column"><div class="page">P0</div><div class="page active">P1</div><div class="page">P2</div><div class="page">P3</div></div></div><div class="flow-arrow"><i class="packet"></i></div><div><b>물리 프레임</b><div class="memory-column"><div class="page">F7</div><div class="page">F2</div><div class="page active">F9</div><div class="page">F4</div></div></div></div><div class="legend">MMU가 페이지 테이블을 보고 P1을 F9로 번역합니다. 오프셋은 그대로 유지됩니다.</div>';
      if (id === 'OS-26') return '<div class="cycle"><svg viewBox="0 0 560 230" aria-hidden="true"><defs><marker id="arrow" markerWidth="8" markerHeight="8" refX="6" refY="3" orient="auto"><path d="M0,0 L0,6 L7,3 z" fill="var(--orange)"/></marker></defs><path d="M330 55 Q470 70 435 145" marker-end="url(#arrow)"/><path d="M385 176 Q280 225 170 176" marker-end="url(#arrow)"/><path d="M125 142 Q86 65 230 52" marker-end="url(#arrow)"/></svg><div class="flow-node n1">작업 A<small>B의 락을 기다림</small></div><div class="flow-node n2">작업 B<small>C의 락을 기다림</small></div><div class="flow-node n3">작업 C<small>A의 락을 기다림</small></div></div><div class="legend">기다림이 원을 만들면 누구도 먼저 진행할 수 없습니다.</div>';
      if (id === 'NET-05' || id === 'NET-17') {
        const labels = id === 'NET-05' ? ['SYN','SYN + ACK','ACK'] : ['ClientHello + 키','인증서 + 서버 키','Finished'];
        return '<div class="lanes"><div class="lane-head"><span>클라이언트</span><span>서버</span></div>' + labels.map((label, index) => '<div class="message ' + (index % 2 ? 'reverse' : '') + '" data-label="' + label + '" style="--angle:' + (index % 2 ? '-3deg' : '3deg') + '"></div>').join('') + '</div><div class="legend">' + (id === 'NET-17' ? '인증과 키 합의가 끝난 뒤부터 빠른 대칭 키로 데이터를 암호화합니다.' : '양쪽이 서로의 송수신 준비와 초기 순서 번호를 확인합니다.') + '</div>';
      }
      if (id === 'DB-15') return '<div class="tree"><div class="tree-level"><div class="tree-node"><span>30</span><span class="hit">60</span></div></div><div>↙︎　　　　　↓　　　　　↘︎</div><div class="tree-level"><div class="tree-node"><span>10</span><span>20</span></div><div class="tree-node"><span>35</span><span class="hit">50</span></div><div class="tree-node"><span>70</span><span>90</span></div></div></div><div class="legend">한 페이지에 키를 많이 담아 높이를 낮추고, 정렬된 Leaf를 이어 범위도 빠르게 읽습니다.</div>';

      const flows = {
        'OS-01': [['실행 파일','디스크'],['프로세스 생성','PCB · 메모리'],['Ready Queue','CPU 대기'],['명령 실행','User / Kernel']],
        'OS-10': [['작업 A','실행 중'],['문맥 저장','PCB'],['스케줄러','다음 선택'],['작업 B','문맥 복원']],
        'JAVA-03': [['.java','소스'],['javac','.class'],['Class Loader','검증 · 로딩'],['JVM','해석 + JIT']],
        'JAVA-08': [['Eden','새 객체'],['Survivor','살아남음'],['Old','오래 생존'],['회수','Mark · Copy']],
        'DB-01': [['BEGIN','작업 시작'],['UPDATE','변경'],['로그 기록','복구 근거'],['COMMIT','영구 반영']],
        'DB-06': [['행 v1','과거 값'],['트랜잭션 A','스냅샷 읽기'],['행 v2','새 값'],['정리','독자 종료 후']],
        'NET-01': [['DNS','IP 찾기'],['TCP / QUIC','길 열기'],['TLS','신원 · 암호화'],['HTTP','요청 · 응답']],
        'REDIS-03': [['Socket','준비 이벤트'],['Event Loop','명령 선택'],['메모리','짧은 실행'],['응답','다음 연결']],
        'REDIS-10': [['요청','Key 조회'],['Redis Miss','값 없음'],['DB','원본 조회'],['Redis','TTL과 저장']],
        'REDIS-17': [['SET NX PX','Token + 임대'],['임계 작업','TTL 안에'],['Fencing','오래된 쓰기 차단'],['Lua 해제','Token 확인']],
        'KAFKA-02': [['Producer','Record'],['Topic','논리 이름'],['Partition','순서 있는 Log'],['Broker','복제 저장']],
        'KAFKA-07': [['At-most-once','유실 가능'],['At-least-once','중복 가능'],['멱등 처리','Event ID'],['결과','업무상 한 번']],
        'KAFKA-11': [['Event 수신','Offset N'],['DB 반영','멱등 쓰기'],['Offset Commit','N + 1'],['장애 재시도','중복 제거']],
        'KAFKA-22': [['업무 변경','DB Transaction'],['Outbox','같이 Commit'],['Publisher','Kafka 전송'],['Consumer','멱등 반영']],
        'JD-01': [['Control Plane','설정 배포'],['SDK 동기화','Version 확인'],['불변 Snapshot','원자 교체'],['로컬 평가','네트워크 없음']],
        'JD-06': [['고객 요청','Event 생성'],['Bounded Queue','메모리 한도'],['Batcher','N건 또는 T초'],['Dispatcher','비동기 전송']],
        'JD-10': [['Ingestion API','인증 · Quota'],['Kafka','내구성 Buffer'],['Processor','검증 · 중복 제거'],['Storage','원본 · 집계']],
        'JD-15': [['Seed + User ID','같은 입력'],['Hash','균등 숫자'],['10,000 Slots','고정 공간'],['Variation','결정적 그룹']],
        'JD-26': [['Readiness OFF','새 요청 차단'],['Drain','진행 요청 완료'],['Flush','Event · Offset'],['SIGTERM 종료','자원 닫기']]
      };
      const nodes = flows[id] || [];
      if (!nodes.length) return null;
      return '<div class="flow">' + nodes.map((node, index) => (index ? '<div class="flow-arrow"><i class="packet"></i></div>' : '') + '<div class="flow-node">' + node[0] + '<small>' + node[1] + '</small></div>').join('') + '</div>';
    }

    function cardFor(item) {
      const sentences = splitSentences(item.answer);
      const first = sentences.shift() || item.answer;
      const steps = sentences.length ? '<ol class="steps">' + sentences.map((sentence) => '<li>' + escapeHtml(sentence) + '</li>').join('') + '</ol>' : '';
      const terms = glossaryFor(item);
      const termsMarkup = terms.length ? '<span class="answer-label" style="margin-top:18px">영어·용어 먼저 풀기</span><div class="term-list">' + terms.map(([term, meaning]) => '<div class="term"><b>' + escapeHtml(term) + '</b>' + escapeHtml(meaning) + '</div>').join('') + '</div>' : '';
      const easy = item.easy ? '<div class="easy-note"><b>비유로 이해하기</b><br>' + escapeHtml(item.easy) + '</div>' : '';
      const caution = item.caution ? '<div class="caution-note"><b>헷갈리지 않기</b><br>' + escapeHtml(item.caution) + '</div>' : '';
      const diagramContent = diagramFor(item.id);
      const diagram = diagramContent ? '<button class="diagram-button" data-diagram="' + item.id + '">▶ 그림으로 보기</button>' : '';
      return '<details class="question-card" data-id="' + item.id + '"><summary><span class="qid">' + item.id + '</span><span class="question">' + escapeHtml(item.question) + '</span><span class="stars">' + '★'.repeat(item.priority) + '</span></summary><div class="answer-panel"><div class="answer-block"><span class="answer-label">30초 핵심</span><p class="one-line">' + escapeHtml(first) + '</p><span class="answer-label">차근차근 원리</span>' + steps + termsMarkup + easy + caution + '<div class="card-actions">' + statusButtons(item) + diagram + '</div>' + (diagramContent ? '<div class="diagram" id="diagram-' + item.id + '">' + diagramContent + '</div>' : '') + '</div></div></details>';
    }

    function renderQuestions() {
      const items = filteredData();
      resultCount.textContent = items.length + '개 질문';
      if (!items.length) {
        questionsRoot.innerHTML = '<div class="empty">조건에 맞는 질문이 없습니다.<br>검색어나 필터를 바꿔 보세요.</div>';
        return;
      }
      const groups = new Map();
      items.forEach((item) => {
        const key = item.domain + ' · ' + item.topic;
        if (!groups.has(key)) groups.set(key, []);
        groups.get(key).push(item);
      });
      questionsRoot.innerHTML = [...groups.entries()].map(([name, group]) => '<h2 class="topic-heading">' + escapeHtml(name) + '<span>' + group.length + '문항</span></h2>' + group.map(cardFor).join('')).join('');
    }

    function renderPractice() {
      const items = filteredData();
      if (!items.length) {
        practiceStage.innerHTML = '<div class="empty">연습할 질문이 없습니다. 필터를 바꿔 보세요.</div>';
        return;
      }
      state.practiceIndex = Math.min(state.practiceIndex, items.length - 1);
      const item = items[state.practiceIndex];
      const status = state.progress[item.id] || '';
      practiceStage.innerHTML = '<article class="flashcard"><div class="flashcard-top"><span>' + item.id + ' · ' + escapeHtml(item.topic) + '</span><span>' + (state.practiceIndex + 1) + ' / ' + items.length + ' · ' + '★'.repeat(item.priority) + '</span></div><h2 class="flash-question">' + escapeHtml(item.question) + '</h2><button class="primary-action" id="reveal-answer">' + (state.practiceAnswerVisible ? '답변 다시 가리기' : '말해 본 뒤 답변 보기') + '</button><div class="answer-reveal ' + (state.practiceAnswerVisible ? 'visible' : '') + '"><span class="answer-label">모범 답안</span><p>' + escapeHtml(item.answer) + '</p>' + (item.easy ? '<div class="easy-note">' + escapeHtml(item.easy) + '</div>' : '') + '</div><div class="practice-actions"><button class="practice-action" id="previous-question">← 이전</button><div class="right"><button class="status-button review" data-status="review" data-id="' + item.id + '" data-active="' + (status === 'review') + '">↺ 복습</button><button class="status-button" data-status="done" data-id="' + item.id + '" data-active="' + (status === 'done') + '">✓ 이해</button><button class="practice-action" id="next-question">다음 →</button></div></div></article>';
    }

    function updateProgress() {
      const done = Object.values(state.progress).filter((value) => value === 'done').length;
      document.getElementById('progress-text').textContent = done + ' / ' + DATA.length;
      document.getElementById('progress-fill').style.width = (done / DATA.length * 100) + '%';
      localStorage.setItem('cs-interview-progress', JSON.stringify(state.progress));
    }

    function rerender() {
      renderDomains();
      renderQuestions();
      renderPractice();
      updateProgress();
    }

    domainList.addEventListener('click', (event) => {
      const button = event.target.closest('[data-domain]');
      if (!button) return;
      state.domain = button.dataset.domain;
      state.practiceIndex = 0;
      state.practiceAnswerVisible = false;
      rerender();
    });

    document.getElementById('search').addEventListener('input', (event) => {
      state.query = event.target.value;
      state.practiceIndex = 0;
      renderQuestions();
      renderPractice();
    });

    document.querySelector('.priority-filter').addEventListener('click', (event) => {
      const button = event.target.closest('[data-priority]');
      if (!button) return;
      state.priority = Number(button.dataset.priority);
      state.practiceIndex = 0;
      state.practiceAnswerVisible = false;
      document.querySelectorAll('[data-priority]').forEach((item) => item.classList.toggle('active', item === button));
      renderQuestions();
      renderPractice();
    });

    document.querySelector('.view-tabs').addEventListener('click', (event) => {
      const button = event.target.closest('[data-view]');
      if (!button) return;
      state.view = button.dataset.view;
      document.querySelectorAll('[data-view]').forEach((item) => item.classList.toggle('active', item === button));
      listView.classList.toggle('active', state.view === 'list');
      practiceView.classList.toggle('active', state.view === 'practice');
      document.getElementById('sidebar').classList.remove('open');
    });

    questionsRoot.addEventListener('click', (event) => {
      const diagramButton = event.target.closest('[data-diagram]');
      if (diagramButton) {
        const panel = document.getElementById('diagram-' + diagramButton.dataset.diagram);
        panel.classList.toggle('visible');
        diagramButton.textContent = panel.classList.contains('visible') ? '▼ 그림 접기' : '▶ 그림으로 보기';
        return;
      }
      handleStatus(event);
    });

    practiceStage.addEventListener('click', (event) => {
      if (event.target.closest('#reveal-answer')) {
        state.practiceAnswerVisible = !state.practiceAnswerVisible;
        renderPractice();
      } else if (event.target.closest('#previous-question')) {
        const length = filteredData().length;
        state.practiceIndex = (state.practiceIndex - 1 + length) % length;
        state.practiceAnswerVisible = false;
        renderPractice();
      } else if (event.target.closest('#next-question')) {
        const length = filteredData().length;
        state.practiceIndex = (state.practiceIndex + 1) % length;
        state.practiceAnswerVisible = false;
        renderPractice();
      } else {
        handleStatus(event);
      }
    });

    function handleStatus(event) {
      const button = event.target.closest('[data-status][data-id]');
      if (!button) return;
      const { id, status } = button.dataset;
      state.progress[id] = state.progress[id] === status ? '' : status;
      updateProgress();
      if (state.view === 'practice') renderPractice(); else renderQuestions();
    }

    document.getElementById('expand-all').addEventListener('click', (event) => {
      const cards = [...document.querySelectorAll('.question-card')];
      const shouldOpen = cards.some((card) => !card.open);
      cards.forEach((card) => { card.open = shouldOpen; });
      event.target.textContent = shouldOpen ? '현재 결과 모두 접기' : '현재 결과 모두 펼치기';
    });

    document.getElementById('menu-button').addEventListener('click', (event) => {
      const sidebar = document.getElementById('sidebar');
      sidebar.classList.toggle('open');
      event.currentTarget.setAttribute('aria-expanded', String(sidebar.classList.contains('open')));
    });

    rerender();
  </script>
</body>
</html>`;

fs.writeFileSync(path.join(directory, 'cs-interview-guide.html'), html);
console.log(`Generated cs-interview-guide.html with ${data.length} questions.`);

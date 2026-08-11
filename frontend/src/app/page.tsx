import { ArrowDownRight, ArrowUpRight } from '@phosphor-icons/react/dist/ssr'
import Image from 'next/image'
import Link from 'next/link'
import { blogPosts } from '@/content/blog-posts'

const principles = [
  ['01', 'Domain first', '기능보다 먼저 경계와 언어를 설계합니다.'],
  ['02', 'Measure twice', '느낌 대신 지표와 실험으로 판단합니다.'],
  ['03', 'Ship small', '작게 배포하고 빠르게 학습합니다.'],
  ['04', 'Write it down', '결정과 실패를 다음 사람을 위해 남깁니다.'],
]

export default function HomePage() {
  return (
    <main>
      <section className="hero" aria-labelledby="hero-title">
        <div className="hero__intro">
          <span className="micro-label">Backend engineer · Seoul</span>
          <h1 id="hero-title">
            BUILD
            <br />
            BEYOND
            <br />
            CODE
          </h1>
          <p>
            복잡한 문제를 작게 나누고, 오래 버티는 백엔드를 설계합니다.
            실험한 것은 글로 남기고 작은 제품으로 연결합니다.
          </p>
          <Link className="text-cta" href="#about">
            나에 대해 더 보기 <ArrowDownRight aria-hidden="true" />
          </Link>
        </div>

        <div
          className="hero__visual"
          aria-label="도자기와 유리 모듈로 구성된 런타임 조형물"
        >
          <Image
            className="hero-specimen hero-specimen--light"
            src="/images/hero-runtime-light.webp"
            alt="흰 도자기와 투명 유리 모듈, 보라색 캡슐로 구성된 추상 런타임 조형물"
            fill
            priority
            sizes="(max-width: 760px) 100vw, 58vw"
          />
          <Image
            className="hero-specimen hero-specimen--dark"
            src="/images/hero-runtime-dark.webp"
            alt="어두운 흑자기와 투명 유리 모듈, 빛나는 보라색 캡슐로 구성된 추상 런타임 조형물"
            fill
            priority
            sizes="(max-width: 760px) 100vw, 58vw"
          />
          <span className="visual-tag visual-tag--one">Stable systems</span>
          <span className="visual-tag visual-tag--two">Human scale</span>
          <span className="visual-tag visual-tag--three">Measure → Learn</span>
          <span className="visual-index">[ 001 / MODULAR RUNTIME ]</span>
        </div>

        <aside className="hero__aside">
          <div className="availability">
            <span className="availability__dot" aria-hidden="true" />
            Open to good problems
          </div>
          <div className="hero-note">
            <span className="micro-label micro-label--solid">About this site</span>
            <p>
              제품을 만드는 방식과 배운 것을 기록합니다. Shop은 실제 주문 흐름을
              실험하는 작은 놀이터입니다.
            </p>
            <Link href="/shop">Try the shop</Link>
          </div>
        </aside>
      </section>

      <section className="principles" aria-label="일하는 원칙">
        <header className="section-kicker">
          <span>[ Core principles ]</span>
          <span>01—04</span>
        </header>
        <div className="principles__grid">
          {principles.map(([number, title, description]) => (
            <article key={number}>
              <span>{number}</span>
              <h2>{title}</h2>
              <p>{description}</p>
            </article>
          ))}
        </div>
      </section>

      <section id="about" className="about-section">
        <div className="section-kicker">
          <span>[ About Justin ]</span>
          <span>NOW / NEXT</span>
        </div>
        <div className="about-section__copy">
          <h2>좋은 시스템은<br />사람의 판단을 돕습니다.</h2>
          <div>
            <p>
              Java와 Spring을 중심으로 도메인의 규칙이 코드에 선명하게 드러나는
              구조를 고민합니다. 성능 숫자만큼 운영하는 사람의 경험도 중요하게 봅니다.
            </p>
            <p>
              지금은 백엔드의 신뢰성과 프런트엔드의 감각이 만나는 지점을 실험하고
              있습니다. 이 사이트는 그 과정의 공개 노트이자 작은 제품 실험실입니다.
            </p>
          </div>
        </div>
      </section>

      <section className="home-blog">
        <div className="section-kicker">
          <span>[ Latest from the blog ]</span>
          <Link href="/blog">모든 글 보기 <ArrowUpRight aria-hidden="true" /></Link>
        </div>
        <div className="home-blog__grid">
          {blogPosts.slice(0, 3).map((post, index) => (
            <Link className="post-card" href={`/blog/${post.slug}`} key={post.slug}>
              <span className="post-card__index">0{index + 1}</span>
              <div>
                <span className="micro-label">{post.category} · {post.readingTime}</span>
                <h2>{post.title}</h2>
                <p>{post.summary}</p>
              </div>
              <ArrowUpRight className="post-card__arrow" aria-hidden="true" />
            </Link>
          ))}
        </div>
      </section>

      <section className="shop-callout">
        <span className="micro-label micro-label--solid">Commerce playground</span>
        <h2>읽고, 둘러보고,<br />가짜로 주문해 보세요.</h2>
        <p>백엔드가 연결되면 실제 주문 API를, 연결되지 않으면 데모 주문을 사용합니다.</p>
        <Link className="text-cta" href="/shop">
          Shop 열기 <ArrowUpRight aria-hidden="true" />
        </Link>
      </section>
    </main>
  )
}

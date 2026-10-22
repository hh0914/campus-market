// 生成演示商品插图（离线可用，避免依赖外部图床）
// 用法：node scripts/generate-item-art.mjs
import { mkdir, writeFile } from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const outDir = path.join(root, 'web', 'public', 'images', 'items')

const INK = '#1f2b26'
const FONT = "'PingFang SC','Hiragino Sans GB','Microsoft YaHei',sans-serif"

const palettes = {
  数码电子: { deep: '#0d5140', accent: '#146b52', soft: '#dcece4', light: '#eef6f1' },
  教材书籍: { deep: '#7d5a12', accent: '#b98a24', soft: '#f3e6c4', light: '#fbf4e2' },
  生活电器: { deep: '#a8431a', accent: '#e8622c', soft: '#fbe2d5', light: '#fdf1e9' },
  运动户外: { deep: '#2a6342', accent: '#2f8f6b', soft: '#dcebe0', light: '#eff7f2' },
  服饰鞋包: { deep: '#7f3a48', accent: '#a34a5a', soft: '#f5dfe3', light: '#fcf0f2' },
  美妆个护: { deep: '#6c3872', accent: '#8a4f8f', soft: '#f0e0f1', light: '#faf1fb' },
  乐器: { deep: '#394671', accent: '#4a5a8f', soft: '#e1e5f4', light: '#f1f3fb' },
  其他闲置: { deep: '#4a5750', accent: '#5d6d64', soft: '#e6eae6', light: '#f4f6f4' },
}

const stroke = `stroke="${INK}" stroke-width="3" stroke-linejoin="round" stroke-linecap="round"`

/* ------------------------------- 商品线稿 ------------------------------- */

function tablet(p) {
  return `<g filter="url(#shadow)">
    <rect x="130" y="36" width="140" height="228" rx="20" fill="#2b3934" ${stroke}/>
    <rect x="139" y="46" width="122" height="208" rx="12" fill="url(#screen)"/>
    <rect x="149" y="58" width="102" height="64" rx="8" fill="#ffffff" opacity=".38"/>
    <rect x="149" y="140" width="66" height="10" rx="5" fill="${p.accent}" opacity=".45"/>
    <rect x="149" y="160" width="94" height="8" rx="4" fill="${p.accent}" opacity=".26"/>
    <rect x="149" y="178" width="80" height="8" rx="4" fill="${p.accent}" opacity=".2"/>
    <circle cx="200" cy="238" r="4" fill="#cfe4da" opacity=".9"/>
  </g>`
}

function mouse(p) {
  return `<g filter="url(#shadow)">
    <path d="M200 54 C242 54 264 102 264 168 C264 226 238 256 200 256 C162 256 136 226 136 168 C136 102 158 54 200 54 Z" fill="#fbfdfb" ${stroke}/>
    <path d="M200 56 V126" fill="none" stroke="${INK}" stroke-width="2.5" opacity=".5"/>
    <rect x="193" y="82" width="14" height="30" rx="7" fill="${p.accent}" opacity=".85"/>
    <path d="M138 152 H262" fill="none" stroke="${INK}" stroke-width="2" opacity=".16"/>
  </g>`
}

function books() {
  return `<g filter="url(#shadow)" transform="rotate(-5 200 165)">
    <rect x="112" y="96" width="176" height="40" rx="6" fill="#e8622c" ${stroke}/>
    <rect x="124" y="106" width="150" height="6" rx="3" fill="#fff" opacity=".75"/>
    <rect x="124" y="118" width="86" height="5" rx="2.5" fill="#fff" opacity=".5"/>
    <rect x="106" y="140" width="188" height="44" rx="6" fill="#146b52" ${stroke}/>
    <rect x="118" y="151" width="162" height="6" rx="3" fill="#fff" opacity=".75"/>
    <rect x="118" y="164" width="98" height="5" rx="2.5" fill="#fff" opacity=".5"/>
    <rect x="114" y="188" width="178" height="42" rx="6" fill="#d9a441" ${stroke}/>
    <rect x="126" y="199" width="152" height="6" rx="3" fill="#fff" opacity=".75"/>
    <rect x="126" y="212" width="80" height="5" rx="2.5" fill="#fff" opacity=".5"/>
    <path d="M288 100 V132 M294 144 V180 M292 192 V226" fill="none" stroke="#fffdf8" stroke-width="4" opacity=".55"/>
  </g>`
}

function papers(p) {
  return `<g filter="url(#shadow)" transform="rotate(5 200 160)">
    <rect x="112" y="62" width="158" height="186" rx="5" fill="#fffdf6" ${stroke}/>
    <rect x="128" y="82" width="74" height="8" rx="4" fill="${p.accent}" opacity=".85"/>
    <g fill="none" stroke="#c9c2b1" stroke-width="4" stroke-linecap="round">
      <path d="M128 108 H254"/><path d="M128 124 H254"/><path d="M128 140 H254"/>
      <path d="M128 156 H222"/><path d="M128 186 H254"/><path d="M128 202 H238"/><path d="M128 218 H254"/>
    </g>
    <circle cx="244" cy="82" r="12" fill="#e8622c" opacity=".9"/>
    <path d="M239 82 l4 4 7 -8" fill="none" stroke="#fff" stroke-width="2.6" stroke-linecap="round"/>
  </g>`
}

function pot(p) {
  return `<g filter="url(#shadow)">
    <path d="M120 122 H280 L266 232 C264 244 254 252 242 252 H158 C146 252 136 244 134 232 Z" fill="#f8faf7" ${stroke}/>
    <ellipse cx="200" cy="122" rx="80" ry="17" fill="${p.soft}" ${stroke}/>
    <ellipse cx="200" cy="120" rx="60" ry="9" fill="#fff" opacity=".75"/>
    <path d="M280 144 C310 148 312 192 282 198" fill="none" ${stroke}/>
    <path d="M120 144 C90 148 88 192 118 198" fill="none" ${stroke}/>
    <rect x="168" y="90" width="64" height="16" rx="8" fill="${p.accent}" ${stroke}/>
    <path d="M170 78 C164 68 174 62 169 52" fill="none" stroke="${p.accent}" stroke-width="3" stroke-linecap="round" opacity=".55"/>
    <path d="M200 76 C194 64 204 58 199 44" fill="none" stroke="${p.accent}" stroke-width="3" stroke-linecap="round" opacity=".4"/>
    <path d="M230 78 C224 68 234 62 229 52" fill="none" stroke="${p.accent}" stroke-width="3" stroke-linecap="round" opacity=".55"/>
    <circle cx="200" cy="222" r="10" fill="${p.accent}" opacity=".85"/>
    <path d="M196 222 h8" stroke="#fff" stroke-width="2" stroke-linecap="round"/>
  </g>`
}

function rackets(p) {
  const one = (tx, ty, rot, handle) => `<g transform="translate(${tx} ${ty}) rotate(${rot})">
    <ellipse cx="0" cy="-34" rx="40" ry="52" fill="#f9fcf9" ${stroke}/>
    <ellipse cx="0" cy="-34" rx="27" ry="38" fill="none" stroke="${p.accent}" stroke-width="1.1" opacity=".45"/>
    <path d="M-27 -64 L24 -4 M-27 -20 L18 -62 M-27 -6 L2 -64 M-8 -74 L24 -32" fill="none" stroke="${p.accent}" stroke-width="1" opacity=".5"/>
    <rect x="-7" y="14" width="14" height="64" rx="6" fill="${handle}" ${stroke}/>
    <rect x="-8" y="30" width="16" height="7" rx="3" fill="${INK}" opacity=".2"/>
  </g>`
  return `<g filter="url(#shadow)">
    ${one(156, 198, -22, p.accent)}
    ${one(252, 188, 24, '#d9a441')}
    <g transform="translate(318 106) rotate(18)">
      <path d="M0 0 L-14 34 H14 Z" fill="#fffdf8" stroke="${INK}" stroke-width="2.5" stroke-linejoin="round"/>
      <path d="M-9 34 L0 54 L9 34 Z" fill="${p.soft}" stroke="${INK}" stroke-width="2.5" stroke-linejoin="round"/>
      <path d="M-10 12 H10 M-12 22 H12" fill="none" stroke="${p.accent}" stroke-width="1.4" opacity=".6"/>
    </g>
  </g>`
}

function jacket(p) {
  return `<g filter="url(#shadow)">
    <path d="M200 54 C186 54 176 60 170 72 L140 82 L112 150 L136 164 L152 128 L152 246 C170 254 230 254 248 246 L248 128 L264 164 L288 150 L260 82 L230 72 C224 60 214 54 200 54 Z" fill="${p.soft}" ${stroke}/>
    <path d="M200 58 V250" fill="none" stroke="${INK}" stroke-width="2" stroke-dasharray="5 4" opacity=".5"/>
    <path d="M200 62 C188 76 188 92 200 104 C212 92 212 76 200 62 Z" fill="#fffdf8" opacity=".8"/>
    <path d="M152 128 L140 164 M248 128 L260 164" fill="none" stroke="${INK}" stroke-width="2" opacity=".28"/>
    <path d="M168 168 H196 M204 168 H232" fill="none" stroke="${INK}" stroke-width="2" opacity=".22"/>
    <rect x="168" y="186" width="28" height="20" rx="4" fill="none" stroke="${INK}" stroke-width="2" opacity=".32"/>
    <rect x="204" y="186" width="28" height="20" rx="4" fill="none" stroke="${INK}" stroke-width="2" opacity=".32"/>
    <circle cx="200" cy="88" r="4" fill="${p.accent}"/>
  </g>`
}

function kindle() {
  return `<g filter="url(#shadow)">
    <rect x="134" y="48" width="132" height="204" rx="14" fill="#3b4541" ${stroke}/>
    <rect x="144" y="60" width="112" height="164" rx="5" fill="#f2f0e4"/>
    <g fill="#8f9a91">
      <rect x="156" y="78" width="70" height="7" rx="3.5"/>
      <rect x="156" y="94" width="88" height="5" rx="2.5" opacity=".7"/>
      <rect x="156" y="106" width="82" height="5" rx="2.5" opacity=".7"/>
      <rect x="156" y="118" width="88" height="5" rx="2.5" opacity=".7"/>
      <rect x="156" y="140" width="52" height="5" rx="2.5" opacity=".5"/>
      <rect x="156" y="152" width="88" height="5" rx="2.5" opacity=".55"/>
      <rect x="156" y="164" width="76" height="5" rx="2.5" opacity=".55"/>
      <rect x="156" y="176" width="88" height="5" rx="2.5" opacity=".55"/>
    </g>
    <circle cx="200" cy="238" r="4.5" fill="#cfd8d1"/>
  </g>`
}

function keyboard(p) {
  let keys = ''
  for (let r = 0; r < 3; r += 1) {
    for (let c = 0; c < 11; c += 1) {
      keys += `<rect x="${104 + c * 18}" y="${136 + r * 18}" width="14" height="12" rx="3" fill="#fffdf8" stroke="${INK}" stroke-width="1.2" opacity=".92"/>`
    }
  }
  return `<g filter="url(#shadow)">
    <rect x="86" y="120" width="228" height="94" rx="10" fill="${p.soft}" ${stroke}/>
    ${keys}
    <rect x="150" y="190" width="100" height="12" rx="3" fill="#fffdf8" stroke="${INK}" stroke-width="1.2"/>
    <rect x="104" y="136" width="14" height="12" rx="3" fill="${p.accent}" stroke="${INK}" stroke-width="1.2"/>
  </g>`
}

function lamp(p) {
  return `<g filter="url(#shadow)">
    <ellipse cx="186" cy="248" rx="58" ry="13" fill="${p.soft}" ${stroke}/>
    <ellipse cx="186" cy="245" rx="34" ry="7" fill="#fff" opacity=".55"/>
    <path d="M186 240 L186 172 L246 126" fill="none" stroke="${INK}" stroke-width="6" stroke-linecap="round"/>
    <circle cx="186" cy="172" r="7" fill="${p.accent}" ${stroke}/>
    <path d="M232 118 L292 92 L304 140 L244 164 Z" fill="${p.accent}" ${stroke}/>
    <path d="M246 164 L206 244 L318 244 Z" fill="url(#cone)"/>
    <path d="M240 162 L252 140" fill="none" stroke="#fff" stroke-width="3" opacity=".55"/>
  </g>`
}

function bike(p) {
  return `<g filter="url(#shadow)" fill="none" stroke="${INK}" stroke-width="4" stroke-linecap="round" stroke-linejoin="round">
    <circle cx="128" cy="196" r="46"/>
    <circle cx="272" cy="196" r="46"/>
    <circle cx="128" cy="196" r="6" fill="${p.accent}" stroke="none"/>
    <circle cx="272" cy="196" r="6" fill="${p.accent}" stroke="none"/>
    <path d="M128 196 L178 118 L242 118 L272 196"/>
    <path d="M178 118 L200 196 L128 196 M242 118 L200 196"/>
    <path d="M166 108 H194" stroke-width="6"/>
    <path d="M242 118 L238 102 H262" stroke-width="5"/>
    <circle cx="200" cy="196" r="9" fill="#fffdf8"/>
    <path d="M200 205 V216 M188 216 H212" stroke-width="3"/>
    <g stroke="${p.accent}" stroke-width="1.5" opacity=".45">
      <path d="M128 150 V242 M82 196 H174 M96 164 L160 228 M96 228 L160 164"/>
      <path d="M272 150 V242 M226 196 H318 M240 164 L304 228 M240 228 L304 164"/>
    </g>
  </g>`
}

function guitar(p) {
  return `<g filter="url(#shadow)" transform="rotate(-10 200 160)">
    <circle cx="172" cy="196" r="58" fill="${p.soft}" ${stroke}/>
    <circle cx="172" cy="146" r="46" fill="${p.soft}" ${stroke}/>
    <circle cx="172" cy="176" r="18" fill="${INK}"/>
    <rect x="158" y="42" width="28" height="96" rx="4" fill="#d9b27a" ${stroke}/>
    <rect x="148" y="22" width="48" height="26" rx="6" fill="#c99a5c" ${stroke}/>
    <g fill="${INK}">
      <circle cx="146" cy="30" r="4"/><circle cx="146" cy="42" r="4"/>
      <circle cx="198" cy="30" r="4"/><circle cx="198" cy="42" r="4"/>
    </g>
    <g stroke="#f6f2ea" stroke-width="1">
      <path d="M164 44 V236 M168 44 V236 M176 44 V236 M180 44 V236"/>
    </g>
    <rect x="148" y="208" width="48" height="10" rx="3" fill="#8a5b34" stroke="${INK}" stroke-width="2"/>
    <rect x="150" y="190" width="44" height="6" rx="3" fill="#fffdf8" stroke="${INK}" stroke-width="2"/>
  </g>`
}

function cosmetics(p) {
  return `<g filter="url(#shadow)">
    <rect x="116" y="132" width="48" height="110" rx="10" fill="#fdf7f4" ${stroke}/>
    <rect x="128" y="112" width="24" height="22" rx="4" fill="${p.accent}" ${stroke}/>
    <rect x="138" y="98" width="8" height="16" rx="3" fill="${p.accent}"/>
    <rect x="128" y="166" width="24" height="42" rx="4" fill="${p.soft}"/>
    <rect x="182" y="182" width="58" height="60" rx="10" fill="#fffdf8" ${stroke}/>
    <rect x="178" y="170" width="66" height="16" rx="6" fill="${p.accent}" ${stroke}/>
    <rect x="196" y="200" width="30" height="24" rx="3" fill="${p.soft}"/>
    <rect x="254" y="146" width="42" height="96" rx="8" fill="#fdf7f4" ${stroke}/>
    <rect x="262" y="128" width="26" height="18" rx="4" fill="${p.accent}" ${stroke}/>
    <path d="M254 170 L296 158 V180 L254 192 Z" fill="${p.soft}" stroke="${INK}" stroke-width="2"/>
    <circle cx="140" cy="158" r="4" fill="${p.accent}" opacity=".6"/>
    <circle cx="290" cy="214" r="4" fill="${p.accent}" opacity=".6"/>
  </g>`
}

function tent(p) {
  return `<g filter="url(#shadow)">
    <path d="M0 250 H400" fill="none" stroke="${INK}" stroke-opacity=".15" stroke-width="2"/>
    <path d="M200 62 L96 244 H304 Z" fill="${p.soft}" ${stroke}/>
    <path d="M200 100 L146 244 H254 Z" fill="${p.accent}" opacity=".22" stroke="${INK}" stroke-width="2" stroke-linejoin="round"/>
    <path d="M200 100 V244" fill="none" stroke="${INK}" stroke-width="2" opacity=".5"/>
    <path d="M96 244 L64 258 M304 244 L336 258" fill="none" stroke="${INK}" stroke-width="2" stroke-dasharray="4 4" opacity=".6"/>
    <circle cx="318" cy="74" r="16" fill="#f3e2ae"/>
    <g fill="#f3e2ae" opacity=".8">
      <circle cx="76" cy="66" r="2.5"/><circle cx="106" cy="44" r="2"/><circle cx="136" cy="72" r="2"/>
      <circle cx="330" cy="120" r="2"/><circle cx="356" cy="44" r="2.5"/>
    </g>
  </g>`
}

function monitor(p) {
  return `<g filter="url(#shadow)">
    <rect x="92" y="62" width="216" height="140" rx="9" fill="#2c3833" ${stroke}/>
    <rect x="101" y="71" width="198" height="122" rx="4" fill="url(#screen)"/>
    <rect x="112" y="82" width="72" height="46" rx="5" fill="#ffffff" opacity=".55"/>
    <rect x="192" y="82" width="96" height="10" rx="5" fill="${p.accent}" opacity=".35"/>
    <rect x="192" y="100" width="80" height="8" rx="4" fill="${p.accent}" opacity=".25"/>
    <rect x="192" y="116" width="90" height="8" rx="4" fill="${p.accent}" opacity=".2"/>
    <rect x="112" y="140" width="176" height="40" rx="5" fill="${p.accent}" opacity=".14"/>
    <path d="M188 202 H212 L218 238 H182 Z" fill="#3b4742" ${stroke}/>
    <rect x="152" y="238" width="96" height="10" rx="5" fill="#3b4742" ${stroke}/>
    <rect x="124" y="258" width="152" height="12" rx="5" fill="#e8ece7" stroke="${INK}" stroke-width="2" opacity=".8"/>
  </g>`
}

function coat(p) {
  return `<g filter="url(#shadow)">
    <path d="M200 52 C184 52 172 60 166 74 L132 86 L104 152 L130 166 L148 132 L148 250 C168 258 232 258 252 250 L252 132 L270 166 L296 152 L268 86 L234 74 C228 60 216 52 200 52 Z" fill="#f4eee2" ${stroke}/>
    <path d="M200 56 V252" fill="none" stroke="${INK}" stroke-width="2" stroke-dasharray="5 4" opacity=".42"/>
    <g fill="none" stroke="${p.accent}" stroke-width="2" opacity=".38">
      <path d="M150 156 H250"/><path d="M149 176 H251"/><path d="M149 196 H251"/><path d="M150 216 H250"/>
      <path d="M120 124 L142 136"/><path d="M258 136 L280 124"/>
    </g>
    <path d="M170 74 C176 84 186 90 200 90 C214 90 224 84 230 74" fill="none" stroke="${INK}" stroke-width="2.5" opacity=".5"/>
    <circle cx="200" cy="104" r="4" fill="${p.accent}"/>
  </g>`
}

/* --------------------------------- 组合 --------------------------------- */

const items = [
  { slug: 'ipad-air-4', title: 'iPad Air 4 64G 深空灰', category: '数码电子', art: tablet },
  { slug: 'mx-master-3s', title: '罗技 MX Master 3S 鼠标', category: '数码电子', art: mouse },
  { slug: 'advanced-math', title: '高等数学 同济第七版 上下册', category: '教材书籍', art: books },
  { slug: 'kaoyan-english', title: '考研英语真题 2010-2025', category: '教材书籍', art: papers },
  { slug: 'rice-cooker', title: '小米电煮锅 1.2L', category: '生活电器', art: pot },
  { slug: 'badminton-set', title: '迪卡侬羽毛球拍 对拍', category: '运动户外', art: rackets },
  { slug: 'north-face-jacket', title: '北面冲锋衣 男款 M 码', category: '服饰鞋包', art: jacket },
  { slug: 'kindle-paperwhite', title: 'Kindle Paperwhite 4 8G', category: '数码电子', art: kindle },
  { slug: 'keychron-k2', title: 'Keychron K2 机械键盘', category: '数码电子', art: keyboard },
  { slug: 'desk-lamp', title: '护眼台灯 国AA级', category: '生活电器', art: lamp },
  { slug: 'giant-bike', title: '捷安特山地车 21 速', category: '运动户外', art: bike },
  { slug: 'yamaha-guitar', title: '雅马哈民谣吉他 F310', category: '乐器', art: guitar },
  { slug: 'cosmetics-set', title: '化妆品九件套 全新未拆', category: '美妆个护', art: cosmetics },
  { slug: 'camping-tent', title: '露营帐篷 双人 防雨', category: '运动户外', art: tent },
  { slug: 'monitor-27', title: '27 寸 2K 显示器 IPS', category: '数码电子', art: monitor },
  { slug: 'winter-coat', title: '冬季棉服 L 码 米白', category: '服饰鞋包', art: coat },
]

const variants = [
  { transform: 'translate(0 0)', chip: '校园实拍' },
  { transform: 'translate(-56 -34) scale(1.3)', chip: '成色细节' },
  { transform: 'translate(29 21) scale(0.85)', chip: '配件一览' },
]

function compose(item, variantIndex) {
  const p = palettes[item.category]
  const v = variants[variantIndex]
  const annotations = variantIndex === 2
    ? `<g fill="none" stroke="${p.accent}" stroke-width="1.4" stroke-dasharray="4 5" opacity=".5">
        <circle cx="304" cy="94" r="30"/>
        <path d="M326 72 L358 48"/>
        <circle cx="304" cy="94" r="2.6" fill="${p.accent}" stroke="none"/>
      </g>`
    : ''

  return `<svg xmlns="http://www.w3.org/2000/svg" width="800" height="600" viewBox="0 0 400 300" role="img" aria-label="${item.title}">
  <defs>
    <linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0" stop-color="${p.light}"/>
      <stop offset="0.55" stop-color="#fffdf8"/>
      <stop offset="1" stop-color="${p.soft}"/>
    </linearGradient>
    <linearGradient id="screen" x1="0" y1="0" x2="0.6" y2="1">
      <stop offset="0" stop-color="#f4fbf7"/>
      <stop offset="1" stop-color="${p.soft}"/>
    </linearGradient>
    <linearGradient id="cone" x1="0" y1="0" x2="0" y2="1">
      <stop offset="0" stop-color="#ffe9b8" stop-opacity=".75"/>
      <stop offset="1" stop-color="#ffe9b8" stop-opacity=".05"/>
    </linearGradient>
    <pattern id="dots" width="22" height="22" patternUnits="userSpaceOnUse">
      <circle cx="2" cy="2" r="1.3" fill="${p.accent}" opacity=".12"/>
    </pattern>
    <filter id="shadow" x="-30%" y="-30%" width="160%" height="160%">
      <feDropShadow dx="0" dy="9" stdDeviation="8" flood-color="${INK}" flood-opacity=".16"/>
    </filter>
  </defs>

  <rect width="400" height="300" fill="url(#bg)"/>
  <rect width="400" height="300" fill="url(#dots)"/>
  <circle cx="332" cy="60" r="72" fill="${p.accent}" opacity=".07"/>
  <circle cx="66" cy="250" r="52" fill="${p.accent}" opacity=".06"/>
  <path d="M0 238 C96 202 152 268 248 234 S360 198 400 216 L400 300 L0 300 Z" fill="${p.accent}" opacity=".05"/>
  <ellipse cx="200" cy="260" rx="116" ry="15" fill="${INK}" opacity=".07"/>

  <g transform="${v.transform}">${item.art(p)}</g>
  ${annotations}

  <g transform="translate(16 16)">
    <rect width="76" height="25" rx="12.5" fill="#fffdf8" opacity=".92" stroke="${p.accent}" stroke-opacity=".28"/>
    <text x="38" y="17" text-anchor="middle" font-size="11.5" font-weight="600" fill="${p.deep}" font-family="${FONT}">${v.chip}</text>
  </g>
  <text x="384" y="289" text-anchor="end" font-size="9.5" letter-spacing="1.4" fill="${INK}" opacity=".34" font-family="Georgia,serif">校园集 · CAMPUS MARKET</text>
</svg>`
}

await mkdir(outDir, { recursive: true })
let count = 0
for (const item of items) {
  for (let i = 0; i < variants.length; i += 1) {
    await writeFile(path.join(outDir, `${item.slug}-${i + 1}.svg`), compose(item, i), 'utf8')
    count += 1
  }
}
console.log(`generated ${count} illustrations in ${outDir}`)

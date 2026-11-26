const LS = localStorage;
let LANG = LS.lang || 'en';
let THEME = LS.theme || (matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light');
document.documentElement.dataset.theme = THEME;

const I = {
en: {
 app:'PreServe', tag:'Predict food waste before it happens', nav_dashboard:'Dashboard', nav_inventory:'Inventory', nav_actions:'Actions', nav_insights:'Insights', nav_impact:'Impact', nav_shops:'My Shops', logout:'Logout',
 login:'Login', register:'Create account', email:'Email', password:'Password (min 6)', name:'Your name', shopName:'Shop name', city:'City', noAcc:"New here? Create an account", haveAcc:'Already registered? Login', demo:'Demo: demo@preserve.com / demo123',
 loading:'Loading...', error:'Something went wrong', save:'Save', cancel:'Cancel', close:'Close', view:'View', units:'units', days:'days', day:'Day {n}', none:'None',
 d_title:'Dashboard', d_sub:'AI waste-risk overview of your shop', d_msg:'{n} products are at high risk of becoming unsold', d_ok:'No product is at high risk right now',
 s_products:'Products', s_units:'Units in stock', s_value:'Stock value', s_var:'Value at risk', s_risky:'High-risk products', s_active:'Active actions',
 d_dist:'Risk distribution', d_top:'Top at-risk products (by value)', d_reorder:'Reorder / stock-out alerts', d_noreorder:'No reorder needed now',
 i_title:'Inventory', i_add:'+ Add product', i_import:'Import CSV', i_search:'Search product...', i_all:'All', i_cat:'Category', i_price:'Price (₹)', i_qty:'Quantity', i_exp:'Expiry date', i_est:'Expected daily sales (optional)',
 i_stock:'Stock', i_left:'Days to expiry', i_avg:'AI daily sales', i_risk:'Risk', i_leftover:'Predicted leftover', i_sell:'Sells out in', i_addstock:'Add stock', i_sale:'Record sale', i_promo:'Promotion active', i_date:'Date',
 i_csvhelp:'CSV columns: name,category,price,quantity,days_remaining,est_daily(optional). First row = header.', i_imported:'{n} products imported',
 i_eval:'AI evaluation of new stock', i_next3:'Expected sales — next 3 days', i_sellout:'Stock will sell out in about {n} days (around {d})', i_selloutfar:'Stock will last more than {n} days', i_noleft:'No leftover expected before expiry',
 p_waste:'Waste probability', p_score:'Risk score', p_exp:'Expected sales before expiry', p_left:'Predicted unsold', p_fore:'Sales history and AI forecast', p_month:'Past sales by month (avg/day)', p_recs:'AI recommendations',
 p_batches:'Stock batches', p_expiry:'Expiry', p_trend:'Demand trend', p_basis:'Forecast basis', p_ly:'Last year (same season)', p_lyna:'No last-year data yet', p_promo:'Promotion uplift', p_start:'Start this action', p_disc:'Discount %', p_hist:'History', p_fc:'AI forecast',
 TREND_UP:'Rising', TREND_DOWN:'Falling', TREND_STABLE:'Stable', BASIS_HISTORY:'Own sales history', BASIS_ESTIMATE:'Your estimate (new product)', BASIS_CATEGORY:'Category average (new product)', BASIS_DEFAULT:'Default (new product)',
 LOW:'Low', MEDIUM:'Medium', HIGH:'High', VERY_HIGH:'Very high', NONE:'Out of stock',
 r_NORMAL:'Sales are healthy. Continue normal selling.', r_DISCOUNT:'Give a {pct}% discount/promotion now — expected to clear about {saved} units that would otherwise go unsold.',
 r_BUNDLE:'Create a bundle offer with a fast-selling product from the same category.', r_VISIBILITY:'Move it to the front/eye-level shelf with a sign (past tests: about +{pct}% sales).',
 r_TRANSFER:'Transfer about {units} units to another branch with higher demand.', r_REORDER:'Stock will finish in about {days} days. Reorder about {qty} units by {by}.',
 r_REORDER_NOW:'Out of stock! Reorder about {qty} units immediately.', r_SEASON_UP:'Season effect: demand rises about {pct}% next month — plan stock.', r_SEASON_DOWN:'Season effect: demand falls about {pct}% next month — avoid over-ordering.',
 r_NEW_BASIS:'New product: forecast uses a category average or your estimate. It becomes more accurate as you record sales.', URGENT:'URGENT', SOON:'SOON',
 a_title:'Action tracking', a_sub:'Record what you did and measure the result', a_type:'Action', a_status:'Status', a_saved:'Units saved', a_complete:'Mark complete', a_sold:'Units sold since action', a_none:'No actions yet. Start one from a product page.',
 DISCOUNT:'Discount', PROMOTION:'Promotion', BUNDLE:'Bundle offer', VISIBILITY:'Better visibility', TRANSFER:'Transfer', ACTIVE:'Active', COMPLETED:'Completed',
 n_title:'Insights: seasonality and past demand', n_sub:'Month-wise demand index by category (1.0 = normal). Purple bars = your real sales data.', n_idx:'Seasonal index', n_real:'Your avg daily sales', n_now:'Current month',
 m_title:'Impact dashboard', m_sub:'Measured from the actions recorded on PreServe', m_prods:'Products saved from waste', m_kg:'Food saved (kg)', m_val:'Inventory value recovered', m_int:'Interventions', m_done:'Completed', m_public:'Public impact profile', m_scan:'Scan to see this shop’s food-waste prevention impact', m_open:'Open public page',
 h_title:'My shops', h_add:'Add new shop', h_cur:'Current', h_use:'Use this shop', h_code:'Shop code', pub_badge:'Food Waste Prevention Partner', pub_kg:'kg potentially wasted food prevented', pub_pr:'products saved', pub_val:'inventory value recovered', pub_act:'prevention actions', pub_note:'These numbers are generated from actions recorded on the PreServe platform.',
 Biscuits:'Biscuits',Snacks:'Snacks',Cereals:'Cereals',Dairy:'Dairy',Beverages:'Beverages',Sauces:'Sauces','Instant Food':'Instant Food',Breakfast:'Breakfast',Spreads:'Spreads',Staples:'Staples',Other:'Other'},
hi: {
 app:'प्रीसर्व', tag:'खाने की बर्बादी होने से पहले ही पहचानें', nav_dashboard:'डैशबोर्ड', nav_inventory:'इन्वेंटरी', nav_actions:'एक्शन', nav_insights:'इनसाइट्स', nav_impact:'इम्पैक्ट', nav_shops:'मेरी दुकानें', logout:'लॉगआउट',
 login:'लॉगिन', register:'खाता बनाएं', email:'ईमेल', password:'पासवर्ड (कम से कम 6)', name:'आपका नाम', shopName:'दुकान का नाम', city:'शहर', noAcc:'नए हैं? खाता बनाएं', haveAcc:'पहले से खाता है? लॉगिन करें', demo:'डेमो: demo@preserve.com / demo123',
 loading:'लोड हो रहा है...', error:'कुछ गड़बड़ हो गई', save:'सेव करें', cancel:'रद्द करें', close:'बंद करें', view:'देखें', units:'यूनिट', days:'दिन', day:'दिन {n}', none:'कोई नहीं',
 d_title:'डैशबोर्ड', d_sub:'आपकी दुकान का AI वेस्ट-रिस्क सारांश', d_msg:'{n} प्रोडक्ट के बिना बिके रहने का ज़्यादा खतरा है', d_ok:'अभी कोई प्रोडक्ट ज़्यादा जोखिम में नहीं है',
 s_products:'प्रोडक्ट', s_units:'स्टॉक (यूनिट)', s_value:'स्टॉक की कीमत', s_var:'जोखिम में कीमत', s_risky:'हाई-रिस्क प्रोडक्ट', s_active:'चालू एक्शन',
 d_dist:'रिस्क का वितरण', d_top:'सबसे ज़्यादा जोखिम वाले प्रोडक्ट (कीमत अनुसार)', d_reorder:'दोबारा मंगाने / स्टॉक खत्म अलर्ट', d_noreorder:'अभी दोबारा मंगाने की ज़रूरत नहीं',
 i_title:'इन्वेंटरी', i_add:'+ प्रोडक्ट जोड़ें', i_import:'CSV इम्पोर्ट', i_search:'प्रोडक्ट खोजें...', i_all:'सभी', i_cat:'कैटेगरी', i_price:'कीमत (₹)', i_qty:'मात्रा', i_exp:'एक्सपायरी तारीख', i_est:'रोज़ की अनुमानित बिक्री (वैकल्पिक)',
 i_stock:'स्टॉक', i_left:'एक्सपायरी में दिन', i_avg:'AI रोज़ की बिक्री', i_risk:'रिस्क', i_leftover:'बचने का अनुमान', i_sell:'खत्म होने में', i_addstock:'स्टॉक जोड़ें', i_sale:'बिक्री दर्ज करें', i_promo:'प्रमोशन चालू था', i_date:'तारीख',
 i_csvhelp:'CSV कॉलम: name,category,price,quantity,days_remaining,est_daily(वैकल्पिक)। पहली पंक्ति हेडर होगी।', i_imported:'{n} प्रोडक्ट इम्पोर्ट हुए',
 i_eval:'नए स्टॉक का AI मूल्यांकन', i_next3:'अगले 3 दिन की अनुमानित बिक्री', i_sellout:'स्टॉक लगभग {n} दिन में खत्म होगा (करीब {d})', i_selloutfar:'स्टॉक {n} दिन से ज़्यादा चलेगा', i_noleft:'एक्सपायरी से पहले कुछ बचने की उम्मीद नहीं',
 p_waste:'बर्बादी की संभावना', p_score:'रिस्क स्कोर', p_exp:'एक्सपायरी तक अनुमानित बिक्री', p_left:'बिना बिका अनुमान', p_fore:'बिक्री इतिहास और AI पूर्वानुमान', p_month:'महीने के हिसाब से पिछली बिक्री (औसत/दिन)', p_recs:'AI सुझाव',
 p_batches:'स्टॉक बैच', p_expiry:'एक्सपायरी', p_trend:'मांग का रुझान', p_basis:'पूर्वानुमान का आधार', p_ly:'पिछला साल (इसी सीज़न में)', p_lyna:'पिछले साल का डेटा अभी नहीं है', p_promo:'प्रमोशन से बढ़त', p_start:'यह एक्शन शुरू करें', p_disc:'डिस्काउंट %', p_hist:'इतिहास', p_fc:'AI पूर्वानुमान',
 TREND_UP:'बढ़ रही', TREND_DOWN:'घट रही', TREND_STABLE:'स्थिर', BASIS_HISTORY:'अपनी बिक्री का इतिहास', BASIS_ESTIMATE:'आपका अनुमान (नया प्रोडक्ट)', BASIS_CATEGORY:'कैटेगरी औसत (नया प्रोडक्ट)', BASIS_DEFAULT:'डिफ़ॉल्ट (नया प्रोडक्ट)',
 LOW:'कम', MEDIUM:'मध्यम', HIGH:'ज़्यादा', VERY_HIGH:'बहुत ज़्यादा', NONE:'स्टॉक खत्म',
 r_NORMAL:'बिक्री अच्छी है। सामान्य रूप से बेचते रहें।', r_DISCOUNT:'अभी {pct}% डिस्काउंट/प्रमोशन दें — इससे लगभग {saved} यूनिट बिकेंगी जो वरना बची रह जातीं।',
 r_BUNDLE:'उसी कैटेगरी के तेज़ बिकने वाले प्रोडक्ट के साथ बंडल ऑफर बनाएं।', r_VISIBILITY:'इसे आगे/आंखों की ऊंचाई वाली शेल्फ पर साइन के साथ रखें (पहले लगभग +{pct}% बिक्री बढ़ी)।',
 r_TRANSFER:'लगभग {units} यूनिट ज़्यादा मांग वाली दूसरी ब्रांच में भेजें।', r_REORDER:'स्टॉक लगभग {days} दिन में खत्म होगा। {by} तक लगभग {qty} यूनिट दोबारा मंगाएं।',
 r_REORDER_NOW:'स्टॉक खत्म है! तुरंत लगभग {qty} यूनिट मंगाएं।', r_SEASON_UP:'सीज़न का असर: अगले महीने मांग लगभग {pct}% बढ़ेगी — स्टॉक की योजना बनाएं।', r_SEASON_DOWN:'सीज़न का असर: अगले महीने मांग लगभग {pct}% घटेगी — ज़्यादा ऑर्डर न करें।',
 r_NEW_BASIS:'नया प्रोडक्ट: पूर्वानुमान कैटेगरी औसत या आपके अनुमान पर है। बिक्री दर्ज करने पर यह और सटीक होगा।', URGENT:'तुरंत', SOON:'जल्द',
 a_title:'एक्शन ट्रैकिंग', a_sub:'आपने क्या किया दर्ज करें और नतीजा मापें', a_type:'एक्शन', a_status:'स्थिति', a_saved:'बचाई गई यूनिट', a_complete:'पूरा करें', a_sold:'एक्शन के बाद बिकी यूनिट', a_none:'अभी कोई एक्शन नहीं। प्रोडक्ट पेज से शुरू करें।',
 DISCOUNT:'डिस्काउंट', PROMOTION:'प्रमोशन', BUNDLE:'बंडल ऑफर', VISIBILITY:'बेहतर डिस्प्ले', TRANSFER:'ट्रांसफर', ACTIVE:'चालू', COMPLETED:'पूरा',
 n_title:'इनसाइट्स: सीज़न और पिछली मांग', n_sub:'कैटेगरी के अनुसार महीने की मांग का सूचकांक (1.0 = सामान्य)। बैंगनी बार = आपकी असली बिक्री।', n_idx:'सीज़न सूचकांक', n_real:'आपकी औसत रोज़ की बिक्री', n_now:'चालू महीना',
 m_title:'इम्पैक्ट डैशबोर्ड', m_sub:'प्रीसर्व पर दर्ज एक्शन से मापा गया', m_prods:'बर्बादी से बचे प्रोडक्ट', m_kg:'बचाया गया खाना (किलो)', m_val:'वापस मिली इन्वेंटरी कीमत', m_int:'एक्शन (कुल)', m_done:'पूरे हुए', m_public:'सार्वजनिक इम्पैक्ट प्रोफ़ाइल', m_scan:'इस दुकान का फूड-वेस्ट रोकथाम इम्पैक्ट देखने के लिए स्कैन करें', m_open:'पब्लिक पेज खोलें',
 h_title:'मेरी दुकानें', h_add:'नई दुकान जोड़ें', h_cur:'चालू', h_use:'यह दुकान चुनें', h_code:'दुकान कोड', pub_badge:'फूड वेस्ट प्रिवेंशन पार्टनर', pub_kg:'किलो संभावित खाने की बर्बादी रोकी', pub_pr:'प्रोडक्ट बचाए', pub_val:'इन्वेंटरी कीमत वापस मिली', pub_act:'रोकथाम एक्शन', pub_note:'ये आंकड़े प्रीसर्व प्लेटफ़ॉर्म पर दर्ज एक्शन से अपने-आप बने हैं।',
 Biscuits:'बिस्किट',Snacks:'स्नैक्स',Cereals:'सीरियल',Dairy:'डेयरी',Beverages:'पेय पदार्थ',Sauces:'सॉस','Instant Food':'इंस्टेंट फूड',Breakfast:'नाश्ता',Spreads:'स्प्रेड',Staples:'राशन',Other:'अन्य'}
};
I.hi['Instant Food'] = 'इंस्टेंट फूड';

function t(k, p) {
  let s = (I[LANG] && I[LANG][k]) || I.en[k] || k;
  if (p) for (const x in p) s = s.split('{' + x + '}').join(p[x]);
  return s;
}
function applyI18n(root = document) {
  root.querySelectorAll('[data-i18n]').forEach(e => e.textContent = t(e.dataset.i18n));
  root.querySelectorAll('[data-ph]').forEach(e => e.placeholder = t(e.dataset.ph));
  document.documentElement.lang = LANG;
}
function setLang(l) { LS.lang = l; location.reload(); }
function toggleTheme() {
  THEME = THEME === 'dark' ? 'light' : 'dark'; LS.theme = THEME;
  document.documentElement.dataset.theme = THEME;
  if (window.onTheme) window.onTheme();
  const b = document.getElementById('themeBtn'); if (b) b.textContent = THEME === 'dark' ? '☀️' : '🌙';
}
const money = n => '₹' + Math.round(n || 0).toLocaleString('en-IN');
const esc = s => String(s ?? '').replace(/[&<>"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));
const badge = r => `<span class="badge b-${r}">${t(r)}</span>`;
const catName = c => t(c);
const css = v => getComputedStyle(document.documentElement).getPropertyValue(v).trim();

function toast(msg, err) {
  const d = document.createElement('div'); d.className = 'toast' + (err ? ' err' : ''); d.textContent = msg;
  document.body.appendChild(d); setTimeout(() => d.remove(), 3200);
}
async function api(path, body, method) {
  const h = { 'Content-Type': 'application/json' };
  if (LS.token) h.Authorization = 'Bearer ' + LS.token;
  const r = await fetch('/api' + path, { method: method || (body ? 'POST' : 'GET'), headers: h, body: body ? JSON.stringify(body) : undefined });
  let d = null; try { d = await r.json(); } catch (e) {}
  if (r.status === 401 && !path.startsWith('/auth') && !path.startsWith('/public')) { LS.removeItem('token'); location = 'login.html'; throw new Error('login'); }
  if (!r.ok) throw new Error((d && d.message) || t('error'));
  return d;
}
function modal(html) {
  const m = document.createElement('div'); m.className = 'modal';
  m.innerHTML = `<div class="card">${html}</div>`;
  m.addEventListener('mousedown', e => { if (e.target === m) m.remove(); });
  document.body.appendChild(m); applyI18n(m); return m;
}
const shops = () => JSON.parse(LS.shops || '[]');
function shopId() { const s = shops(); const id = +LS.shopId; return (s.find(x => x.id === id) || s[0] || {}).id; }
function saveSession(d) { LS.token = d.token; LS.userName = d.name; LS.shops = JSON.stringify(d.shops); if (!shopId()) delete LS.shopId; }

/* page shell: top bar + navigation. Call shell('dashboard') */
function shell(active) {
  if (!LS.token) { location = 'login.html'; return false; }
  const pages = ['dashboard', 'inventory', 'actions', 'insights', 'impact', 'shops'];
  const sh = shops(), cur = shopId();
  const bar = document.createElement('div'); bar.className = 'top';
  bar.innerHTML = `<span class="logo">🌿 <span data-i18n="app"></span></span>
    <div class="nav">${pages.map(p => `<a href="${p}.html" class="${p === active ? 'on' : ''}" data-i18n="nav_${p}"></a>`).join('')}</div>
    <select id="shopSel" title="Shop">${sh.map(s => `<option value="${s.id}" ${s.id === cur ? 'selected' : ''}>${esc(s.name)}</option>`).join('')}</select>
    <select id="langSel"><option value="en">English</option><option value="hi">हिन्दी</option></select>
    <button id="themeBtn" class="btn sec" onclick="toggleTheme()">${THEME === 'dark' ? '☀️' : '🌙'}</button>
    <button class="btn sec" onclick="logout()" data-i18n="logout"></button>`;
  document.body.prepend(bar);
  document.getElementById('langSel').value = LANG;
  document.getElementById('langSel').onchange = e => setLang(e.target.value);
  document.getElementById('shopSel').onchange = e => { LS.shopId = e.target.value; location.reload(); };
  applyI18n();
  return true;
}
/* login / register pages have only the two toggles */
function miniBar() {
  const b = document.createElement('div'); b.className = 'top';
  b.innerHTML = `<span class="logo">🌿 <span data-i18n="app"></span></span><select id="langSel"><option value="en">English</option><option value="hi">हिन्दी</option></select><button id="themeBtn" class="btn sec" onclick="toggleTheme()">${THEME === 'dark' ? '☀️' : '🌙'}</button>`;
  document.body.prepend(b);
  document.getElementById('langSel').value = LANG; document.getElementById('langSel').onchange = e => setLang(e.target.value);
  applyI18n();
}
async function logout() { try { await api('/logout', {}); } catch (e) {} ['token', 'shops', 'shopId', 'userName'].forEach(k => LS.removeItem(k)); location = 'login.html'; }

/* translate AI recommendation objects */
function recsHtml(recs) {
  return recs.map(r => {
    const p = Object.assign({}, r.params);
    const warn = ['REORDER', 'REORDER_NOW', 'TRANSFER', 'DISCOUNT'].includes(r.code) ? ' warn' : '';
    const ic = { NORMAL: '✅', DISCOUNT: '🏷️', BUNDLE: '🎁', VISIBILITY: '👀', TRANSFER: '🚚', REORDER: '📦', REORDER_NOW: '🚨', SEASON_UP: '📈', SEASON_DOWN: '📉', NEW_BASIS: 'ℹ️' }[r.code] || '•';
    const pr = p.priority ? ` <span class="badge b-${p.priority === 'URGENT' ? 'VERY_HIGH' : 'HIGH'}">${t(p.priority)}</span>` : '';
    return `<div class="rec${warn}"><span>${ic}</span><span>${esc(t('r_' + r.code, p))}${pr}</span></div>`;
  }).join('');
}
/* shared AI-evaluation card (used after adding stock and on product page) */
function evalHtml(a) {
  const so = a.sellOutDays > 0 && a.sellOutDays < 120 ? t('i_sellout', { n: a.sellOutDays, d: a.sellOutDate }) : t('i_selloutfar', { n: 120 });
  return `<h2>${esc(a.name)} ${badge(a.risk)}</h2>
   <div class="grid g4"><div class="card stat"><b>${a.wasteProb}%</b><span data-i18n="p_waste"></span></div>
   <div class="card stat"><b>${a.leftover}</b><span data-i18n="p_left"></span></div>
   <div class="card stat"><b>${a.avgDaily}</b><span data-i18n="i_avg"></span></div></div>
   <p><b data-i18n="i_next3"></b>: ${a.next3.map((v, i) => t('day', { n: i + 1 }) + ' = <b>' + v + '</b>').join(' &nbsp;·&nbsp; ')}</p>
   <p>${a.stock === 0 ? '' : so}</p>${recsHtml(a.recs)}`;
}
const CATS = ['Biscuits', 'Snacks', 'Cereals', 'Dairy', 'Beverages', 'Sauces', 'Instant Food', 'Breakfast', 'Spreads', 'Staples', 'Other'];

/*
 * Regression tests for the quick-calculator cores in app/src/main/assets/index.html.
 * Reference values were produced with SciPy 1.18 (scipy.stats) and cross-checked.
 * Usage: node tests/test_calculators.js app/src/main/assets/index.html
 */
const fs = require('fs');
const html = fs.readFileSync(process.argv[2] || 'index.html', 'utf8');

// Collect every top-level <script> body and evaluate it in one sandbox with a minimal DOM stub.
const scripts = [...html.matchAll(/<script>([\s\S]*?)<\/script>/g)].map(m => m[1]);
const vm = require('vm');
const el = () => new Proxy(function () {}, { get: (t, k) => k === 'value' ? '' : k === 'style' || k === 'classList' || k === 'dataset' ? el() : el(), apply: () => el(), set: () => true });
const ctx = {
  console, Math, Number, String, Array, Object, JSON, Set, Map, Error, Date, Float64Array, Promise, RegExp, Symbol, parseFloat, parseInt, isNaN, Infinity, NaN,
  setTimeout: () => 0, clearTimeout: () => {}, requestAnimationFrame: () => 0,
  document: { getElementById: () => el(), querySelector: () => el(), querySelectorAll: () => [], addEventListener: () => {}, createElement: () => el(), body: el(), documentElement: el() },
  window: {}, navigator: {}, location: { hash: '', protocol: 'file:' }, history: { state: null, pushState() {}, replaceState() {} },
  localStorage: { getItem: () => null, setItem() {}, removeItem() {} }, addEventListener() {},
  matchMedia: () => ({ matches: false, addEventListener() {} }), Blob: function () {}, URL: { createObjectURL: () => '' },
};
ctx.window = ctx; vm.createContext(ctx);
let loaded = 0;
for (const s of scripts) { try { vm.runInContext(s, ctx); loaded++; } catch (e) { /* UI bootstrap may fail in the stub; function declarations are already hoisted */ } }
const f = (name) => ctx[name] || vm.runInContext(`typeof ${name}==='function'?${name}:undefined`, ctx);

const near = (a, b, tol) => Math.abs(a - b) <= tol * Math.max(1, Math.abs(b));
const T = [];
const t = (name, fn) => T.push({ name, fn });

t('z-score', () => near(f('zScoreCore')(15, 10, 2).z, 2.5, 1e-12));
t('normInv(0.975)', () => near(f('normInv')(0.975), 1.959963984540054, 1e-8));
t('tQuantile df=1 p=.9995 (=636.619)', () => near(f('tQuantile')(0.9995, 1), 636.6192487687897, 1e-6));
t('CI 95% n=5 uses t(4)=2.776', () => { const r = f('ciMeanCore')([5.1, 4.9, 6.2, 5.8, 5.5], 0.95); return near(r.lower, 4.848865906114782, 1e-9) && near(r.upper, 6.151134093885218, 1e-9); });
t('CI 99.9% n=2 (very wide, df=1)', () => { const r = f('ciMeanCore')([5.1, 4.9], 0.999); return near(r.upper - r.mean, 63.66192487687898, 1e-6); });
t('CI rejects 95 (percent) input', () => { try { f('ciMeanCore')([1, 2, 3], 95); return false; } catch (e) { return /between 0 and 1/.test(e.message); } });
t('Mann-Whitney exact p', () => { const r = f('mannWhitneyCore')([5.1, 4.9, 6.2, 5.8, 5.5], [6.8, 7.1, 6.5, 7.4, 6.9, 7.7]); return r.U === 0 && near(r.p, 0.004329004329004329, 1e-9) && r.method === 'exact'; });
t('Mann-Whitney ties (normal approx)', () => near(f('mannWhitneyCore')([1, 2, 2, 3, 4, 5, 5, 6], [2, 3, 3, 5, 7, 8, 8, 9, 10]).p, 0.0727309640929567, 1e-9));
t('Wilcoxon exact p', () => { const r = f('wilcoxonCore')([1.5, -0.5, 2.2, 0.7, -1.1, 3.0, 0.4, -0.2, 1.9, 2.4]); return r.W === 9 && near(r.p, 0.064453125, 1e-12); });
t('Wilcoxon ties (normal approx)', () => near(f('wilcoxonCore')([3, 2, 3, 2, 4, 3]).p, 0.03350525679082817, 1e-9));
t('Kruskal-Wallis (tie-corrected)', () => { const r = f('kruskalWallisCore')([[1, 2, 2, 3], [3, 4, 4, 5, 6], [5, 6, 7, 7]]); return near(r.H, 9.453351955307268, 1e-9) && near(r.p, 0.008855859243165318, 1e-8); });
t('Power exact t (n=10, d=5, sd=8)', () => near(f('powerMeanCore')(10, 5, 8, 0.05).power, 0.423134, 1e-5));
t('Power exact t (n=5, d=5, sd=8)', () => near(f('powerMeanCore')(5, 5, 8, 0.05).power, 0.191826, 1e-5));
t('Power exact t (n=30, d=5, sd=10, a=.01)', () => near(f('powerMeanCore')(30, 5, 10, 0.01).power, 0.502555, 1e-5));
t('Power validates n', () => { try { f('powerMeanCore')(1, 5, 8, 0.05); return false; } catch (e) { return true; } });
t('Hardy-Weinberg uses estimated allele freq', () => { const r = f('hardyWeinbergCore')([50, 40, 10]); return near(r.chi, 0.226757369614513, 1e-9) && near(r.pv, 0.633938683881345, 1e-8) && r.df === 1; });
t('Hardy-Weinberg perfect fit', () => f('hardyWeinbergCore')([50, 100, 50], 1).chi < 1e-12);
t('Hardy-Weinberg needs 3 genotypes', () => { try { f('hardyWeinbergCore')([10, 20]); return false; } catch (e) { return true; } });
t('Q-Q correlation (Blom)', () => near(f('qqDataCore')([10, 12, 11, 14, 13, 15, 12]).r, 0.9920374362519838, 1e-9));
t('Tukey critical q (k=3, df=9)', () => near(f('tukeyHSDCore')([[5.1, 4.9, 6.2, 5.8], [6.8, 7.1, 6.5, 7.4, 6.9], [8.1, 7.7, 8.4]]).qcrit, 3.948492203457504, 1e-5));
t('Tukey pair p-value', () => near(f('tukeyHSDCore')([[5.1, 4.9, 6.2, 5.8], [6.8, 7.1, 6.5, 7.4, 6.9], [8.1, 7.7, 8.4]]).pairs[2].p, 0.017984207166903632, 1e-4));
t('Trailing comma does not add a phantom 0', () => { ctx.document.getElementById = (id) => ({ value: '1,2,3,' }); const a = f('parseNumbersText')('1,2,3,'); return a.length === 3; });
t('Non-numeric token is rejected', () => { try { f('parseNumbersText')('1,abc,3'); return false; } catch (e) { return /Non-numeric/.test(e.message); } });
t('Back-button guard disabled', () => /^function armHomeGuard\(\)\{return;/.test(f('armHomeGuard').toString()));

let passed = 0, failed = 0;
T.forEach(({ name, fn }) => {
  try { const ok = fn(); console.log(ok ? 'PASS:' : 'FAIL:', name); ok ? passed++ : failed++; }
  catch (e) { failed++; console.log('ERROR:', name, e.message); }
});
console.log(`\n${passed}/${T.length} passed`);
process.exit(failed > 0 ? 1 : 0);

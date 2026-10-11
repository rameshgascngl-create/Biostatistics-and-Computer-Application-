const fs = require('fs');
const html = fs.readFileSync(process.argv[2] || 'index.html', 'utf8');

function extractFn(name) {
  const start = html.indexOf('function ' + name);
  if (start < 0) return null;
  let depth = 0, i = html.indexOf('{', start);
  for (; i < html.length; i++) {
    if (html[i] === '{') depth++;
    else if (html[i] === '}') { depth--; if (depth === 0) break; }
  }
  return html.slice(start, i + 1);
}

const sandbox = {};
['zScoreCore','ciMeanCore','mannWhitneyCore','kruskalWallisCore','powerMeanCore','hardyWeinbergCore','qqDataCore','tukeyHSDCore'].forEach(name => {
  const code = extractFn(name);
  if (code) try { eval(code.replace('function ' + name, 'sandbox.' + name + ' = function')); } catch(e) {}
});

const tests = [
  { name: 'z-score', fn: () => sandbox.zScoreCore(15, 10, 2).z, expected: 2.5 },
  { name: 'CI mean', fn: () => sandbox.ciMeanCore([10,12,14,16,18], 0.95).mean, expected: 14 },
  { name: 'Mann-Whitney U', fn: () => sandbox.mannWhitneyCore([1,2,3],[4,5,6]).U, expected: 0 },
  { name: 'Kruskal H>0', fn: () => sandbox.kruskalWallisCore([[1,2],[3,4],[5,6]]).H > 0, expected: true },
  { name: 'Power>0', fn: () => sandbox.powerMeanCore(30,5,10,0.05).power > 0, expected: true },
  { name: 'HW df', fn: () => sandbox.hardyWeinbergCore([50,100,50],1).df, expected: 1 },
  { name: 'QQ n', fn: () => sandbox.qqDataCore([1,2,3,4,5]).n, expected: 5 },
  { name: 'Tukey pairs', fn: () => sandbox.tukeyHSDCore([[1,2],[3,4],[5,6]]).pairs.length, expected: 3 },
];

let passed = 0, failed = 0;
tests.forEach(t => {
  try {
    const result = t.fn();
    const ok = typeof t.expected === 'boolean' ? result === t.expected : Math.abs(result - t.expected) < 0.01;
    console.log(ok ? 'PASS:' : 'FAIL:', t.name, ok ? '' : `(got ${result})`);
    ok ? passed++ : failed++;
  } catch (e) { failed++; console.log('ERROR:', t.name, e.message); }
});
console.log(`\n${passed}/${tests.length} passed`);
process.exit(failed > 0 ? 1 : 0);

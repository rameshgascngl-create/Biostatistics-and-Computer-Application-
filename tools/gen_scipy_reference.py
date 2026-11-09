"""Generate SciPy reference values for the Kotlin calculators.

Run:  python3 tools/gen_scipy_reference.py > app/src/test/resources/scipy_reference.json
Requires numpy and scipy (>= 1.11). The reference inputs are fixed so the output is reproducible.
"""
import json
import numpy as np
from scipy import stats
from scipy.stats import contingency

R = {}
f = lambda v: float(v)

# ---- distributions -------------------------------------------------------
R["dist"] = {
    "tcdf": [[t, d, f(stats.t.cdf(t, d))] for t, d in [(0.5, 3), (2.0, 15), (-1.7, 8), (3.4, 30), (1.0, 1), (6.0, 2.5)]],
    "chi2sf": [[x, d, f(stats.chi2.sf(x, d))] for x, d in [(2.0, 1), (5.99, 2), (11.07, 5), (30.0, 12), (0.4, 3)]],
    "fsf": [[x, a, b, f(stats.f.sf(x, a, b))] for x, a, b in [(6.0, 2, 18), (3.1, 3, 20), (1.2, 5, 40), (10.5, 4, 9)]],
    "normcdf": [[z, f(stats.norm.cdf(z))] for z in [-3.1, -1.0, 0.0, 0.7, 1.96, 2.5]],
    "norminv": [[p, f(stats.norm.ppf(p))] for p in [0.001, 0.025, 0.3, 0.5, 0.9, 0.975, 0.9999]],
    "tinv": [[p, d, f(stats.t.ppf(p, d))] for p, d in [(0.975, 4), (0.995, 10), (0.9, 2), (0.975, 120), (0.95, 1)]],
    "nctcdf": [[t, d, dl, f(stats.nct.cdf(t, d, dl))] for t, d, dl in [(2.0, 15, 2.5), (2.262, 9, 1.5), (1.7, 24, 3.0)]],
    "srange": [[q, k, d, f(stats.studentized_range.cdf(q, k, d))] for q, k, d in [(3.5, 3, 15), (4.0, 4, 20), (2.9, 3, 30), (5.0, 5, 12)]],
}
fish = lambda a, b, c, d: f(stats.fisher_exact([[a, b], [c, d]])[1])
R["fisher"] = [[a, b, c, d, fish(a, b, c, d)] for a, b, c, d in [(3, 1, 1, 3), (8, 2, 1, 5), (12, 5, 29, 2)]]

# ---- descriptive ---------------------------------------------------------
x = np.array([12.4, 13.1, 13.5, 14.0, 14.0, 14.7, 15.2, 16.8])
R["descriptive"] = {"x": x.tolist(), "n": len(x), "mean": f(x.mean()), "median": f(np.median(x)),
    "q1": f(np.percentile(x, 25)), "q3": f(np.percentile(x, 75)), "sampleVar": f(x.var(ddof=1)),
    "sampleSD": f(x.std(ddof=1)), "popVar": f(x.var()), "se": f(stats.sem(x)),
    "cv": f(100 * x.std(ddof=1) / x.mean()), "skew": f(stats.skew(x, bias=False)),
    "ciLow": f(stats.t.interval(0.95, len(x) - 1, loc=x.mean(), scale=stats.sem(x))[0]),
    "ciHigh": f(stats.t.interval(0.95, len(x) - 1, loc=x.mean(), scale=stats.sem(x))[1])}

# ---- t tests -------------------------------------------------------------
a = np.array([18.2, 17.5, 19.1, 16.8, 18.9, 17.7, 18.4, 19.0])
b = np.array([16.1, 17.0, 15.8, 16.9, 17.2, 15.5, 16.6, 16.0, 17.1, 16.4])
pa = np.array([42., 45., 40., 48., 44., 46.]); pb = np.array([46., 49., 42., 53., 47., 50.])
def tres(r, ci):
    return {"t": f(r.statistic), "df": f(r.df), "p": f(r.pvalue), "ciLow": f(ci.low), "ciHigh": f(ci.high)}
r1 = stats.ttest_1samp(a, 17.0)
R["t_one"] = {"a": a.tolist(), "mu": 17.0, **tres(r1, r1.confidence_interval())}
r2 = stats.ttest_ind(a, b, equal_var=True)
R["t_pooled"] = {"a": a.tolist(), "b": b.tolist(), **tres(r2, r2.confidence_interval())}
r3 = stats.ttest_ind(a, b, equal_var=False)
R["t_welch"] = {"a": a.tolist(), "b": b.tolist(), **tres(r3, r3.confidence_interval())}
r4 = stats.ttest_rel(pb, pa)
R["t_paired"] = {"a": pa.tolist(), "b": pb.tolist(), **tres(r4, r4.confidence_interval())}

# ---- chi-square ----------------------------------------------------------
obs = [42, 18, 8, 52]; exp = [30, 30, 30, 30]
g = stats.chisquare(obs, exp)
R["chi_gof"] = {"obs": obs, "exp": exp, "chi": f(g.statistic), "p": f(g.pvalue), "df": 3}
m = np.array([[30, 70], [15, 85]])
c = stats.chi2_contingency(m, correction=False)
R["chi_ind_2x2"] = {"m": m.tolist(), "chi": f(c.statistic), "p": f(c.pvalue), "df": int(c.dof),
    "v": f(np.sqrt(c.statistic / m.sum())), "fisher": fish(30, 70, 15, 85)}
m3 = np.array([[20, 15, 30], [25, 20, 10], [18, 22, 14]])
c3 = stats.chi2_contingency(m3, correction=False)
R["chi_ind_3x3"] = {"m": m3.tolist(), "chi": f(c3.statistic), "p": f(c3.pvalue), "df": int(c3.dof),
    "v": f(np.sqrt(c3.statistic / (m3.sum() * 2)))}

# ---- correlation / regression -------------------------------------------
cx = np.array([10, 12, 14, 16, 18, 20.]); cy = np.array([22, 29, 38, 49, 63, 78.])
lr = stats.linregress(cx, cy)
ci = lr.slope + np.array([-1, 1]) * stats.t.ppf(0.975, len(cx) - 2) * lr.stderr
R["corr"] = {"x": cx.tolist(), "y": cy.tolist(), "r": f(lr.rvalue), "slope": f(lr.slope), "intercept": f(lr.intercept),
    "p": f(lr.pvalue), "seb": f(lr.stderr), "slopeLow": f(ci[0]), "slopeHigh": f(ci[1]),
    "spearman": f(stats.spearmanr(cx, cy).statistic)}
sx = np.array([3, 1, 4, 1.5, 5, 9, 2, 6]); sy = np.array([2, 7, 1, 8, 2.5, 8, 4, 5])
R["spearman_ties"] = {"x": sx.tolist(), "y": sy.tolist(), "rho": f(stats.spearmanr(sx, sy).statistic)}

# ---- ANOVA ----------------------------------------------------------------
g1 = [12, 15, 14, 11, 13]; g2 = [17, 19, 16, 18, 20, 18]; g3 = [22, 25, 21, 24, 23]
fo = stats.f_oneway(g1, g2, g3)
allv = np.array(g1 + g2 + g3, float)
ssb = sum(len(g) * (np.mean(g) - allv.mean()) ** 2 for g in (g1, g2, g3))
ssw = sum(((np.array(g) - np.mean(g)) ** 2).sum() for g in (g1, g2, g3))
R["anova"] = {"groups": [g1, g2, g3], "F": f(fo.statistic), "p": f(fo.pvalue), "eta": f(ssb / (ssb + ssw)), "df1": 2, "df2": 13}
t1 = [12, 15, 14, 11, 13]; t2 = [14, 17, 15, 13, 16, 15]; t3 = [16, 19, 15, 17, 18]
tk = stats.tukey_hsd(t1, t2, t3)
R["tukey"] = {"groups": [t1, t2, t3], "p": [[f(tk.pvalue[i][j]) for j in range(3)] for i in range(3)]}

# ---- non-parametric -------------------------------------------------------
mwa = [12.1, 14.3, 9.8, 15.2, 11.0, 13.7]; mwb = [16.5, 18.1, 15.9, 19.2, 17.4, 20.0, 16.8]
mw = stats.mannwhitneyu(mwa, mwb, alternative="two-sided", method="exact")
u1 = float(mw.statistic); u2 = len(mwa) * len(mwb) - u1
R["mw_exact"] = {"a": mwa, "b": mwb, "u": min(u1, u2), "p": f(mw.pvalue)}
mta = [3, 5, 5, 7, 8, 8, 9]; mtb = [6, 8, 10, 10, 12, 13, 15, 14]
mt = stats.mannwhitneyu(mta, mtb, alternative="two-sided", method="asymptotic", use_continuity=True)
R["mw_ties"] = {"a": mta, "b": mtb, "u": min(float(mt.statistic), len(mta) * len(mtb) - float(mt.statistic)), "p": f(mt.pvalue)}
wd = [1.5, -0.4, 2.3, 3.1, -1.2, 0.8, 2.9, 1.7, -0.6, 2.2]
wx = stats.wilcoxon(wd, method="exact", alternative="two-sided")
R["wilcoxon_exact"] = {"d": wd, "w": f(wx.statistic), "p": f(wx.pvalue)}
wdt = [1, -1, 2, 2, 3, -3, 4, 5, 5, 6, 0]
wt = stats.wilcoxon(wdt, method="approx", correction=True, zero_method="wilcox", alternative="two-sided")
R["wilcoxon_ties"] = {"d": wdt, "w": f(wt.statistic), "p": f(wt.pvalue)}
kw = stats.kruskal([2.9, 3.0, 2.5, 2.6, 3.2], [3.8, 2.7, 4.0, 2.4], [2.8, 3.4, 3.7, 2.2, 2.0])
R["kruskal"] = {"groups": [[2.9, 3.0, 2.5, 2.6, 3.2], [3.8, 2.7, 4.0, 2.4], [2.8, 3.4, 3.7, 2.2, 2.0]], "H": f(kw.statistic), "p": f(kw.pvalue)}
kwt = stats.kruskal([1, 2, 2, 3], [2, 3, 4, 4], [4, 5, 5, 6])
R["kruskal_ties"] = {"groups": [[1, 2, 2, 3], [2, 3, 4, 4], [4, 5, 5, 6]], "H": f(kwt.statistic), "p": f(kwt.pvalue)}

# ---- CI, z, power, QQ, HW, sample size, epi -------------------------------
cd = [23.1, 25.4, 22.8, 24.9, 26.0, 23.7, 24.2]
tcrit = stats.t.ppf(0.975, len(cd) - 1); cm = np.mean(cd); cse = stats.sem(cd)
R["ci"] = {"x": cd, "conf": 0.95, "mean": f(cm), "se": f(cse), "tcrit": f(tcrit), "lower": f(cm - tcrit * cse), "upper": f(cm + tcrit * cse),
    "conf99low": f(stats.t.interval(0.99, len(cd) - 1, loc=cm, scale=cse)[0])}
R["z"] = {"x": 24.0, "mean": 20.0, "sd": 4.0, "z": 1.0, "upper": f(stats.norm.sf(1.0))}
def power(n, delta, sd, alpha):
    df = n - 1; ncp = abs(delta) / (sd / np.sqrt(n)); tc = stats.t.ppf(1 - alpha / 2, df)
    return f(1 - stats.nct.cdf(tc, df, ncp) + stats.nct.cdf(-tc, df, ncp))
R["power"] = [[n, d, s, a_, power(n, d, s, a_)] for n, d, s, a_ in [(10, 5, 6, 0.05), (20, 2, 4, 0.05), (30, 1.5, 5, 0.01), (8, 4, 3, 0.05)]]
qd = [12.4, 13.1, 13.5, 14.0, 14.0, 14.7, 15.2, 16.8, 18.0]
qs = np.sort(qd); n = len(qs); zq = stats.norm.ppf((np.arange(1, n + 1) - 0.375) / (n + 0.25))
R["qq"] = {"x": qd, "z": zq.tolist(), "r": f(np.corrcoef(qs, zq)[0, 1])}
hw = [42, 46, 12]
pp = (2 * hw[0] + hw[1]) / (2 * sum(hw)); ex = [pp * pp * sum(hw), 2 * pp * (1 - pp) * sum(hw), (1 - pp) ** 2 * sum(hw)]
chi = sum((o - e) ** 2 / e for o, e in zip(hw, ex))
R["hw"] = {"obs": hw, "p": f(pp), "exp": ex, "chi": f(chi), "pvalue": f(stats.chi2.sf(chi, 1))}
R["samplesize"] = {"mean": [1.96, 6, 2, np.ceil((1.96 * 6 / 2) ** 2).item()],
                   "prop": [1.96, 0.30, 0.05, np.ceil(1.96 ** 2 * 0.3 * 0.7 / 0.05 ** 2).item()]}
rr = contingency.relative_risk(30, 100, 15, 100); rrci = rr.confidence_interval(0.95)
orr = contingency.odds_ratio([[30, 70], [15, 85]], kind="sample"); orci = orr.confidence_interval(0.95)
R["epi"] = {"cells": [30, 70, 15, 85], "rr": f(rr.relative_risk), "rrLow": f(rrci.low), "rrHigh": f(rrci.high),
            "or": f(orr.statistic), "orLow": f(orci.low), "orHigh": f(orci.high), "rd": 0.15}
print(json.dumps(R, indent=1))

import csv
import math
from pathlib import Path
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

root = Path(__file__).resolve().parents[1]
folder = root / "results" / "plots"
folder.mkdir(parents=True, exist_ok=True)
with (root / "results" / "results.csv").open() as file:
    rows = list(csv.DictReader(file))
for row in rows:
    for key in ["n", "m", "dsu_ops", "edge_scans", "comparisons", "max_depth", "result"]:
        row[key] = int(row[key])
    row["operations"] = row["dsu_ops"] + row["edge_scans"] + row["comparisons"]

algorithms = ["Kruskal", "Prim", "Kosaraju", "Kahn", "DagShortestPath"]
colors = ["#2563eb", "#e8590c", "#008577", "#a426ac", "#444444"]
plt.rcParams.update({"font.size": 10, "axes.spines.top": False, "axes.spines.right": False})

def series(algorithm, density):
    return sorted([r for r in rows if r["algorithm"] == algorithm and r["graph"].endswith(density)], key=lambda r: r["n"])

def slope(data, key):
    x = [math.log(r["n"]) for r in data]
    y = [math.log(r[key]) for r in data]
    xm = sum(x) / len(x)
    ym = sum(y) / len(y)
    return sum((a - xm) * (b - ym) for a, b in zip(x, y)) / sum((a - xm) ** 2 for a in x)

fig, ax = plt.subplots(figsize=(9, 4.7), layout="constrained")
for algorithm, color in zip(algorithms, colors):
    for density, style in [("sparse", "-"), ("dense", "--")]:
        data = series(algorithm, density)
        marker = "o" if algorithm != "DagShortestPath" else "x"
        ax.plot([r["n"] for r in data], [r["operations"] for r in data], style, color=color, marker=marker, label=f"{algorithm} {density}", markersize=5)
ax.set(xscale="log", yscale="log", xlabel="Vertices n", ylabel="Counted operations", title="Operations vs graph size")
ax.grid(True, which="both", alpha=0.15)
ax.legend(fontsize=8, ncol=2)
fig.savefig(folder / "operations.png", dpi=200)
plt.close(fig)

fig, axes = plt.subplots(1, 2, figsize=(9, 3.8), layout="constrained", sharey=True)
for ax, density in zip(axes, ["sparse", "dense"]):
    for algorithm, color in zip(algorithms, colors):
        data = series(algorithm, density)
        style = "--" if algorithm in ["Prim", "DagShortestPath"] else "-"
        marker = "x" if algorithm in ["Prim", "DagShortestPath"] else "o"
        ax.plot([r["n"] for r in data], [r["edge_scans"] / r["m"] for r in data], style, marker=marker, color=color, label=algorithm, markersize=8 if algorithm == "Prim" else 5)
    ax.set(xscale="log", xlabel="Vertices n", title=density.capitalize(), ylim=(0, 2.25))
    ax.grid(True, alpha=0.2)
axes[0].set_ylabel("edgeScans / m")
axes[1].legend(fontsize=8, loc="center", bbox_to_anchor=(0.5, 0.65))
fig.savefig(folder / "scans_per_edge.png", dpi=200)
plt.close(fig)

fig, ax = plt.subplots(figsize=(9, 3.4), layout="constrained")
for density, style, color in [("sparse", "-o", colors[0]), ("dense", "--s", colors[1])]:
    data = series("Kruskal", density)
    ax.plot([r["n"] for r in data], [r["edge_scans"] / r["m"] for r in data], style, color=color, label=density)
ax.set(xscale="log", xlabel="Vertices n", ylabel="Examined edges / m", title="Kruskal stops after selecting n - 1 edges", ylim=(0, 1.05))
ax.grid(True, alpha=0.2)
ax.legend()
fig.savefig(folder / "kruskal_share.png", dpi=200)
plt.close(fig)

with (root / "results" / "slopes.csv").open("w", newline="") as file:
    writer = csv.writer(file)
    writer.writerow(["algorithm", "density", "edge_scans_slope", "operations_slope"])
    for algorithm in algorithms:
        for density in ["sparse", "dense"]:
            data = series(algorithm, density)
            writer.writerow([algorithm, density, f"{slope(data, 'edge_scans'):.4f}", f"{slope(data, 'operations'):.4f}"])
print("Created three plots and slopes.csv")

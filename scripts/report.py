import csv
import math
from pathlib import Path
from xml.sax.saxutils import escape
from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.pagesizes import A4
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak, Image

root = Path(__file__).resolve().parents[1]
with (root / "results/results.csv").open() as file:
    rows = list(csv.DictReader(file))
with (root / "results/slopes.csv").open() as file:
    slopes = list(csv.DictReader(file))

def row_for(algorithm, n, density):
    return next(r for r in rows if r["algorithm"] == algorithm and int(r["n"]) == n and r["graph"].endswith(density))

styles = getSampleStyleSheet()
styles.add(ParagraphStyle(name="BodySmall", fontName="Helvetica", fontSize=9, leading=12.5, spaceAfter=7))
styles.add(ParagraphStyle(name="CellSmall", fontName="Helvetica", fontSize=7.5, leading=9.5))
styles.add(ParagraphStyle(name="CaptionSmall", fontName="Helvetica", fontSize=8, leading=10.5, spaceAfter=7, textColor=colors.HexColor("#475569")))
styles["Title"].fontSize = 22
styles["Title"].leading = 26
styles["Title"].textColor = colors.HexColor("#17365b")
styles["Heading1"].fontSize = 15
styles["Heading1"].leading = 19
styles["Heading1"].textColor = colors.HexColor("#17365b")
styles["Heading2"].fontSize = 11
styles["Heading2"].leading = 14
story = []
markdown = []

def heading(text, level=1):
    story.append(Paragraph(escape(text), styles["Heading" + str(level)]))
    markdown.append("#" * (level + 1) + " " + text + "\n")

def paragraph(text, caption=False):
    story.append(Paragraph(escape(text), styles["CaptionSmall" if caption else "BodySmall"]))
    markdown.append(text + "\n")

def table(headers, data, widths):
    cells = [[Paragraph(escape(str(value)), styles["CellSmall"]) for value in line] for line in [headers] + data]
    item = Table(cells, colWidths=widths, repeatRows=1, hAlign="LEFT")
    item.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#e6eef7")),
        ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, colors.HexColor("#f5f7fa")]),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("LEFTPADDING", (0, 0), (-1, -1), 5),
        ("RIGHTPADDING", (0, 0), (-1, -1), 5),
        ("TOPPADDING", (0, 0), (-1, -1), 5),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
        ("LINEBELOW", (0, 0), (-1, 0), 0.6, colors.HexColor("#8098b5"))
    ]))
    story.extend([item, Spacer(1, 7)])
    markdown.append("| " + " | ".join(headers) + " |\n| " + " | ".join(["---"] * len(headers)) + " |")
    markdown.extend("| " + " | ".join(str(value) for value in line) + " |" for line in data)
    markdown.append("")

def chart(name, height):
    story.append(Image(str(root / "results/plots" / name), width=499, height=height))
    markdown.append(f"![{name}](results/plots/{name})\n")

def page():
    story.append(PageBreak())
    markdown.append("\n---\n")

story.append(Paragraph("Assignment 3: Graphs", styles["Title"]))
markdown.append("# Assignment 3: Graphs\n")
paragraph("Temirgaliyev Nurassyl | SE-2523 | Design and Analysis of Algorithms | 22 October 2026")
heading("Implementation and measurement")
paragraph("One adjacency-list Graph class stores directed or undirected integer-weight edges. All six algorithms are implemented in Java. Distances and total weights use long. DSU uses union by size and full iterative path compression. Kosaraju uses explicit arrays as DFS stacks; no DFS is recursive. Prim begins at vertex 0 and restarts at each unvisited vertex to return a forest.")
paragraph("The benchmark uses a fresh Random(42) for each graph, the exact shuffle and generation order from the assignment, five sizes and two densities (m = 4n and 20n). Each size/density creates an undirected graph, a directed graph and a DAG: 30 inputs and 50 algorithm rows. Five measured runs per row give 250 runs. All reference values and required scan counts match. The detailed 50-row specification takes precedence over the final deliverables line mentioning four graphs.")
heading("Complexity", 2)
table(["Operation", "Time", "Extra space", "Reason"], [
    ["DSU find / union", "O(alpha(V)) amortized", "O(V) shared; O(1) per call", "Size balancing limits height; compression flattens repeated paths. makeSet costs O(V)."],
    ["Kruskal", "O(V + E log(E+1) + E alpha(V))", "O(V + E)", "Sort an edge-list copy, then perform at most E unions; DSU stores V entries."],
    ["Prim (lazy heap)", "O(V + E log(E+1))", "O(V + E)", "Scan adjacency lists; push/pop up to E edges. Parallel edges can enlarge the heap."],
    ["Kosaraju", "O(V + E)", "O(V + E)", "Two DFS passes plus a transposed graph; explicit stacks are at most V deep."],
    ["Kahn", "O(V + E)", "O(V)", "Copy maintained indegrees; enqueue each vertex once and inspect each edge once."],
    ["DAG shortest paths", "O(V + E)", "O(V)", "Compute a topological order, then relax every outgoing edge once."],
], [86, 116, 85, 212])
heading("What the counters mean", 2)
paragraph("Here alpha is the inverse Ackermann function. It grows so slowly that find and union have effectively constant amortized cost at practical input sizes. This is a bound over a sequence of operations, not a claim that every individual find takes exactly the same work.")
paragraph("dsuOps counts find calls plus successful unions, not parent-pointer steps. Kruskal calls union once per examined edge; union calls find twice, so dsuOps = 2 edgeScans + selectedEdges. Its comparisons count sorting weight comparisons. Prim comparisons count priority-queue weight comparisons; its scans count both adjacency entries of each undirected edge (2m).")
paragraph("Kosaraju records the two DFS scans (2m), excluding transpose construction. Kahn records m edge scans; indegrees are maintained during graph construction. DAG shortest paths records m relaxation scans, excluding its internal Kahn pass. These exclusions follow the required counter convention, although their work is included in measured time. maxDepth is the largest Kosaraju DFS frame stack; other rows use 0. Counters are stable across the five runs; library comparison counts can vary with a different sorting/heap implementation.")

page()
heading("Correctness and algorithm choice")
heading("Cut property: Kruskal and Prim", 2)
paragraph("Consider a cut that respects the forest already selected, so no selected edge crosses it. Let e be a lightest edge crossing the cut, and take a minimum spanning tree containing the selected forest. If that tree omits e, adding e creates a cycle with another crossing edge f. Since weight(e) is at most weight(f), replacing f by e preserves connectivity and cannot increase the total weight. Therefore the selected forest can safely include e, even when there are ties or negative weights. Kruskal applies this rule to a DSU component when considering the next sorted edge, while Prim applies it between visited and unvisited vertices; repeating independently in each connected component gives a minimum spanning forest.")
heading("Induction on a topological order", 2)
paragraph("Add a virtual source with zero-weight edges to every original vertex of indegree zero. Every vertex of a finite DAG is reachable from such a source, because repeatedly following predecessors must end without a cycle. Initially the source vertices have their correct distance zero. Assume all vertices earlier than v in topological order already have their correct shortest distances. Every predecessor of v occurs earlier, so all possible final edges into v have been relaxed and their minimum gives the correct distance to v. Induction therefore proves all distances after one pass; recording the predecessor on each improvement reconstructs a shortest path.")
paragraph("Negative edges are safe because a DAG has no cycle to repeat, and later vertices cannot feed back into settled earlier vertices. General graphs with negative edges need a method such as Bellman-Ford: a reachable negative cycle can make a finite shortest distance undefined. Negative weights also invalidate Dijkstra's usual guarantee, even without a negative cycle.")
heading("Which algorithm when", 2)
table(["Situation", "Choice", "Why"], [
    ["Many connectivity / merge queries", "DSU", "Almost-constant amortized queries; it does not find graph paths."],
    ["Sparse weighted undirected edge list", "Kruskal", "Sort the existing edges and use DSU to reject cycles."],
    ["Weighted undirected adjacency list", "Prim", "Grow from vertex 0 using the existing neighbors; restart for a forest."],
    ["Denser input, m = 20n, this implementation", "Kruskal here", "Measured comparisons are lower than lazy Prim. Both still have E log E bounds."],
    ["Truly dense simple graph, E near V squared", "Consider O(V squared) array Prim", "Avoid heap overhead; this alternative is not implemented here."],
    ["Directed graph; cycles may exist", "Kosaraju", "Find SCCs; size > 1 or a self-loop marks cyclic dependencies."],
    ["Need a job order or cycle detection", "Kahn", "Returns a valid order or reports a cycle."],
    ["Shortest routes in a DAG", "DAG relaxation", "Linear work and negative edges allowed; reject cyclic input."],
], [158, 123, 218])
heading("Validation", 2)
paragraph("All 23 JUnit 5 tests passed. DSU is checked against simple component labels; MST methods agree on 100 random connected graphs and return n - 1 edges. SCC partitions match BFS reachability in both directions, including n = 200. DAG distances and restored paths match Bellman-Ford with negative weights. Four file fixtures cover connected, disconnected, cyclic and DAG inputs. Extra checks cover empty graphs, self-loops, parallel edges, isolates, invalid input and a 100,000-vertex path whose explicit stack reaches 100,000 without JVM stack overflow.")

page()
heading("Verified results and normalized counters")
paragraph("Both MST algorithms agree in every run. Kahn returns n vertices with pos[u] < pos[v] for every edge. The table contains computed values, checked against the assignment's reference constants before the final CSV is replaced.")
reference = []
ratios = []
for n in [1000, 3162, 10000, 31623, 100000]:
    for density in ["sparse", "dense"]:
        k = row_for("Kruskal", n, density)
        p = row_for("Prim", n, density)
        s = row_for("Kosaraju", n, density)
        d = row_for("DagShortestPath", n, density)
        m = int(k["m"])
        reference.append([f"{n:,}", f"{m:,}", f"{int(k['result']):,}", f"{int(s['result']):,}", f"{int(d['result']):,}"])
        ratios.append([f"{n:,}", density, f"{int(k['dsu_ops'])/m:.3f}", f"{int(k['comparisons'])/(m*math.log2(m)):.3f}", f"{int(p['comparisons'])/(m*math.log2(m)):.3f}", f"{2*m/(n+m):.3f}", f"{m/(n+m):.3f}"])
table(["n", "m", "MST weight", "SCCs", "Sum of DAG distances"], reference, [70, 89, 105, 64, 171])
heading("Counter / growth formula", 2)
table(["n", "Density", "Kruskal DSU / m", "Kruskal cmp / m log2 m", "Prim cmp / m log2 m", "SCC scans / (n+m)", "Kahn / DAG scans / (n+m)"], ratios, [52, 48, 70, 86, 83, 76, 84])
paragraph("Each row's last column applies separately to Kahn and DAG relaxation. Prim scans/(n+m) equals the SCC column because both count 2m. Traversal ratios are exactly constant at a fixed density; heap and sorting ratios stay bounded and of similar scale. Kruskal DSU/m changes because the stopping position changes, but is at most 2 + (n-1)/m on these connected graphs. These measurements support the complexity bounds; finite data and call counts alone do not prove the inverse-Ackermann DSU bound.")
paragraph("The input weight range is only 1..1000, so many edges tie. Sorting all m edges is still required even when Kruskal's DSU loop stops early. Counts of comparisons, scans and DSU calls describe different work and must not be substituted for one another.")

page()
heading("Operations and fitted slopes")
chart("operations.png", 261)
paragraph("Operations = edge_scans + comparisons + dsu_ops. This is a transparent sum of counted events, not a claim that all events cost equal time. The Kahn and DAG lines coincide. Solid lines are sparse; dashed lines are dense.", True)
data = [[r["algorithm"], r["density"], f"{float(r['edge_scans_slope']):.3f}", f"{float(r['operations_slope']):.3f}"] for r in slopes]
table(["Algorithm", "Density", "Scan slope", "Operation slope"], data, [190, 91, 109, 109])
paragraph("Slopes use least-squares regression of log(counter) on log(n), fitted separately for each density across all five sizes. The logarithm base does not change the fitted slope. Prim, Kosaraju, Kahn and DAG edge scans have slope 1.000 because m is proportional to n and their counted scan multipliers are fixed.")
paragraph("Kruskal's scan slopes are slightly above 1 over this finite range because its examined fraction increases, not because scanning includes sorting. The handout associates the scan slope with E log E, but the logarithmic sorting factor belongs to comparisons. The operation slopes include those comparisons; fixed-range equal weights and early stopping can make finite fitted slopes differ from a worst-case asymptotic expectation.")

page()
heading("Scans, early stopping and timing")
chart("scans_per_edge.png", 211)
paragraph("Prim and Kosaraju coincide at 2; Kahn and DAG relaxation coincide at 1. Their flat lines confirm linear counted edge work. Kruskal counts sorted edges only until its tree is complete, so its line is lower and varies with the graph.", True)
chart("kruskal_share.png", 189)
k_small_s = row_for("Kruskal", 1000, "sparse")
k_large_s = row_for("Kruskal", 100000, "sparse")
k_small_d = row_for("Kruskal", 1000, "dense")
k_large_d = row_for("Kruskal", 100000, "dense")
paragraph("Kruskal examines {:.1%} to {:.1%} of sparse edges and {:.1%} to {:.1%} of dense edges between the smallest and largest inputs. The denser graphs offer more inexpensive alternatives, so the tree is completed after a smaller fraction of the sorted list. Kruskal still sorts every edge. Lazy Prim scans all adjacency entries and pays for heap insertions and removals; the CSV shows more weight comparisons than Kruskal at every size. This supports choosing Kruskal for these generated inputs, not a universal claim about all Prim implementations.".format(int(k_small_s["edge_scans"])/int(k_small_s["m"]), int(k_large_s["edge_scans"])/int(k_large_s["m"]), int(k_small_d["edge_scans"])/int(k_small_d["m"]), int(k_large_d["edge_scans"])/int(k_large_d["m"])))
heading("Wall-clock observations", 2)
ks = row_for("Kruskal", 100000, "sparse")["time_ms"]
kd = row_for("Kruskal", 100000, "dense")["time_ms"]
ps = row_for("Prim", 100000, "sparse")["time_ms"]
pd = row_for("Prim", 100000, "dense")["time_ms"]
paragraph(f"The submitted measurements used OpenJDK 25 on Windows, with a 2 GB heap limit and the median of five runs; there was no separate warmup phase. At n = 100,000, Kruskal took {ks} ms sparse and {kd} ms dense; Prim took {ps} ms and {pd} ms. Some small-input timings decrease as n grows because JVM compilation, garbage collection and background load are significant. Generation, validation and output are excluded; internal transpose/topological preparation is included. Other hardware and JVM versions change times, so counters drive the conclusions. Reproduction regenerates measured times rather than promising the same milliseconds.")

def footer(canvas, document):
    canvas.setFont("Helvetica", 8)
    canvas.setFillColor(colors.HexColor("#64748b"))
    canvas.drawString(48, 26, "Temirgaliyev Nurassyl | SE-2523 | DAA Assignment 3")
    canvas.drawRightString(A4[0] - 48, 26, str(document.page))

document = SimpleDocTemplate(str(root / "REPORT.pdf"), pagesize=A4, rightMargin=48, leftMargin=48, topMargin=35, bottomMargin=43, title="Assignment 3: Graphs", author="Temirgaliyev Nurassyl")
document.build(story, onFirstPage=footer, onLaterPages=footer)
(root / "REPORT.md").write_text("\n".join(markdown), encoding="utf-8")
print("Created REPORT.pdf and REPORT.md")

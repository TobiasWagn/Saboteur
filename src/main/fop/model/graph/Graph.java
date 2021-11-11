package fop.model.graph;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 
 * Modelliert einen ungerichteten Graph mit Knoten des Typs {@code V} durch eine Adjazenzliste.
 *
 * @param <V> Typ der Knoten des Graphen
 */
public class Graph<V> {
	
	/** Die Adjazenzliste des Graphen. */
	protected Map<V, Set<V>> G = new HashMap<>();
	
	/**
	 * Erstellt einen leeren ungerichteten Graphen.
	 */
	public Graph() {}
	
	/**
	 * Entfernt alle Knoten und Kanten des Graphen.
	 */
	public void clear() {
		G.clear();
	}
	
	
	// add //
	
	/**
	 * Fügt den übergebenen Knoten hinzu.
	 * @param v der Knoten, der hinzugefügt werden soll
	 */
	public void addVertex(V v) {
		G.putIfAbsent(v, new HashSet<>());
	}
	
	/**
	 * Fügt eine Kante von Knoten {@code x} nach Knoten {@code y} hinzu.
	 * @param x der Startknoten der Kante
	 * @param y der Endknoten der Kante
	 * @return {@code true} wenn die Kante hinzugefügt wurde;
	 *         {@code false} wenn sie bereits existiert hat
	 */
	public boolean addEdge(V x, V y) {
		if(!G.containsKey(x))
			this.addVertex(x);
		if(!G.containsKey(y))
			this.addVertex(y);
		if(G.get(x).contains(y))
			return false;
		G.get(x).add(y);
		G.get(y).add(x);
		return true;
	}
	
	
	// remove //
	
	/**
	 * Entfernt den Knoten {@code v} und alle mit ihm verbundenen Kanten.
	 * @param v der Knoten, der entfernt werden soll
	 * @return {@code true} wenn der Knoten entfernt wurde;
	 *         {@code false} wenn er nicht existiert hat
	 */
	public boolean removeVertex(V v) {
		if(!G.containsKey(v))
			return false;
		Set<V> RemoveEdges = G.get(v);
		for(V vertex : RemoveEdges) {
			Set<V> vertexEdges = G.get(vertex);
			vertexEdges.remove(v);
			G.put(vertex, vertexEdges);
		}
		G.remove(v);
		return true;
	}
	
	/**
	 * Entfernt die Kante von Knoten {@code x} nach Knoten {@code y}.
	 * @param x der Startknoten der Kante
	 * @param y der Endknoten der Kante
	 * @return {@code true} wenn die Kante entfernt wurde;
	 *         {@code false} wenn die Kante nicht existiert hat
	 */
	public boolean removeEdge(V x, V y) {
		if(!G.get(x).contains(y))
			return false;
		G.remove(x, y);
		G.remove(y, x);
		return true;
	}
	
	
	// has //
	
	/**
	 * Prüft, ob der Graph den Knoten {@code v} besitzt.
	 * @param v der zu überprüfende Knoten
	 * @return {@code true} wenn der Knoten existiert; sonst {@code false}
	 */
	public boolean hasVertex(V v) {
		return G.containsKey(v);
	}
	
	/**
	 * Prüft, ob der Graph eine Kante vom Knoten {@code x} zum Knoten {@code y} besitzt.
	 * @param x der Startknoten der Kante
	 * @param y der Endknoten der Kante
	 * @return {@code true} wenn die Kante existiert; sonst {@code false}
	 */
	public boolean hasEdge(V x, V y) {
		return G.containsKey(x) && G.get(x).contains(y);
	}
	
	/**
	 * Prüft, ob ein Pfad vom Knoten {@code x} zum Knoten {@code y} existiert.
	 * @param x der Startknoten des Pfads
	 * @param y der Endknoten des Pfads
	 * @return {@code true} wenn ein Pfad existiert; sonst {@code false}
	 */
	public boolean hasPath(V x, V y) {
		if(x == null || y == null || !G.containsKey(x) || !G.containsKey(y))
			return false;
		if(x.equals(y))
			return true;
		// Graphensuche BFS-Code ist aus Vorlesung ADM bekannt und wird hier implementiert.
		Queue<V> queue = new ArrayDeque<>();
		queue.add(x);
		V currentVertex;	
		Set<V> checked = new HashSet<>();
		while(!queue.isEmpty()) {
			currentVertex = queue.remove();
			if(G.get(currentVertex).contains(y))
				return true;
			else {
				checked.add(currentVertex);
				queue.addAll(G.get(currentVertex));
				queue.removeAll(checked);
			}
		}
		return false;
	}
	
	
	// Collections //
	
	/**
	 * Gibt die Menge aller Knoten zurück.
	 * @return die Menge aller Knoten
	 */
	public Set<V> vertices() {
		return G.keySet();
	}
	
	/**
	 * Gibt die Menge aller benachbarter Knoten des Knoten {@code v} zurück.
	 * @param v der Knoten, dessen Nachbarn zurückgegeben werden sollen
	 * @return die Menge aller benachbarter Knoten
	 */
	public Set<V> getAdjacentVertices(V v) {
		return G.get(v);
	}
	
	/**
	 * Gibt die Menge aller Kanten zurück.
	 * @return die Menge aller Kanten
	 */
	public Set<Edge<V>> edges() {
		Set<Edge<V>> edges = new HashSet<>();
		G.forEach((x, m) -> m.forEach(y -> {
			edges.add(Edge.of(x, y));
		}));
		return edges;
	}
	
	/**
	 * Gibt die Menge aller Kanten zurück, wobei immer nur die Kante in eine Richtung gezählt wird.<br>
	 * <i>Beispiel:</i><br>
	 * Statt {@code [(x, y), (y, x)]} wird {@code [(x, y)]} ausgegeben.
	 * @return die Menge aller Kanten
	 */
	private Set<Edge<V>> singleEdges() {
		Set<Edge<V>> edges = new HashSet<>();
		G.forEach((x, m) -> m.forEach(y -> {
			if (!edges.contains(Edge.of(x, y)) && !edges.contains(Edge.of(y, x))) edges.add(Edge.of(x, y));
		}));
		return edges;
	}
	
	
	// Visualization //
	
	/**
	 * Liefert eine Darstellung des Graphen in {@code dot}-Sprache.<br>
	 * Mittels {@code graph.toDotCode.forEach(System.out::println)} kann dieser Code auf der Konsole ausgegeben werden.<br>
	 * Auf {@code http://webgraphviz.com/} kann der Code dargestellt werden.
	 * @return die einzelnen Zeilen des {@code dot}-Codes
	 */
	public List<String> toDotCode() {
		List<String> l = new ArrayList<>();
		l.add("graph {");
		Comparator<Object> byString = (a, b) -> a.toString().compareTo(b.toString());
		vertices().stream().sorted(byString).forEach(v -> l.add(String.format("\t\"%s\" [label=\"%s\"];", v, v)));
		singleEdges().stream().sorted(byString).forEach(key -> {
			l.add(String.format("\t\"%s\" -- \"%s\";", key.x(), key.y()));
		});
		l.add("}");
		return l;
	}
	
	/**
	 * Zum Debuggen kann hiermit der Graph ausgegeben werden.<br>
	 * Auf {@code http://webgraphviz.com/} kann der Code dargestellt werden.
	 */
	public void printGraph() {
		toDotCode().forEach(System.out::println);
	}
	
	
	// Object //
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		sb.append(vertices().stream().sorted().collect(Collectors.toList()).toString());
		sb.append(", ");
		sb.append("{");
		if (singleEdges().isEmpty())
			sb.append("  ");
		else {
			Comparator<Object> byString = (a, b) -> a.toString().compareTo(b.toString());
			singleEdges().stream().sorted(byString).forEach(key -> {
				sb.append(String.format("%s<->%s, ", key.x(), key.y()));
			});
		}
		sb.replace(sb.length() - 2, sb.length(), "");
		sb.append("}");
		sb.append(")");
		return sb.toString();
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (G == null ? 0 : G.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Graph<?> other = (Graph<?>) obj;
		if (G == null) {
			if (other.G != null) return false;
		} else if (!G.equals(other.G)) return false;
		return true;
	}
	
}

package it.polito.tdpProvaFinale.laBaionettaReader.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleGraph;
import org.jgrapht.graph.SimpleWeightedGraph;

import it.polito.tdpProvaFinale.laBaionettaReader.beans.Articolo;
import it.polito.tdpProvaFinale.laBaionettaReader.beans.Mostrina;
import it.polito.tdpProvaFinale.laBaionettaReader.beans.ParolaChiave;
import it.polito.tdpProvaFinale.laBaionettaReader.beans.Penna;
import it.polito.tdpProvaFinale.laBaionettaReader.dao.ArticoloDAO;

public class Model {

	private ArticoloDAO dao = new ArticoloDAO();

	private Set<Articolo> articoli = new HashSet<>();
	private Set<Penna> penne = new HashSet<>();
	private Set<Mostrina> mostrine = new HashSet<>();

	SimpleGraph<Articolo, DefaultWeightedEdge> grafo;

	private int numArtOld;
	private int numArtNew;

	public List<Articolo> initialize() {
		if (articoli.isEmpty())
			articoli.addAll(dao.getAllArticoli());
		if (mostrine.isEmpty())
			mostrine.addAll(dao.getAllMostrine());
		if (penne.isEmpty())
			penne.addAll(dao.getAllPenne());

		for (Mostrina m : mostrine) {
			Set<Articolo> art = new HashSet<>();
			for (Articolo a : articoli) {
				if (a.getMostrina().equals(m))
					art.add(a);
			}
			m.setArticoli(art);
		}

		for (Penna p : penne) {
			Set<Articolo> art = new HashSet<>();
			for (Articolo a : articoli) {
				if (a.getPenna().equals(p))
					art.add(a);
			}
			p.setArticoli(art);
		}

		creaGrafo();

		return getAllArticoliOrderByDate();
	}

	public Set<Penna> getAllPenne() {
		return penne;
	}

	public Set<Mostrina> getAllMostrine() {
		return mostrine;
	}

	private void creaGrafo() {

		grafo = new SimpleWeightedGraph<Articolo, DefaultWeightedEdge>(DefaultWeightedEdge.class);

		Graphs.addAllVertices(grafo, articoli);

		for (Articolo a : articoli) {
			a.setParoleChiave(dao.getParoleChiave(a));
		}

		for (Articolo a1 : articoli) {
			for (Articolo a2 : articoli) {
				if (a1.hashCode() < a2.hashCode()) {

					int peso = 0;

					for (ParolaChiave pc1 : a1.getParoleChiave()) {
						for (ParolaChiave pc2 : a2.getParoleChiave()) {
							if (pc1.hashCode() < pc2.hashCode()) {

								if (pc1.isSimele(pc2)) {
									peso += pc1.getPeso();
								}
							}
						}

						if (peso > 0) {
							Graphs.addEdge(grafo, a1, a2, peso);
						}
					}
				}
			}
		}
	}

	public Set<Articolo> getAllArticoliFromPenna(Penna p) {
		return p.getAllArticoli();
	}

	public Set<Articolo> getAllArticoliFromMostrina(Mostrina m) {
		return m.getAllArticoli();
	}

	public List<Articolo> getArticoloFromTitolo(String titolo) {
		Set<Articolo> trovati = new HashSet<>();
		for (Articolo a : articoli) {
			if (a.getTitolo().contains(titolo)) {
				trovati.add(a);
			}
		}
		return orderByDate(trovati);
	}

	public List<Articolo> getAllArticoliOrderByDate() {
		List<Articolo> articoliOrdinati = new ArrayList<>();
		articoliOrdinati.addAll(articoli);
		Collections.sort(articoliOrdinati);
		return articoliOrdinati;
	}

	public List<Articolo> orderByDate(Set<Articolo> art) {
		List<Articolo> articoliOrdinati = new ArrayList<>(art);
		Collections.sort(articoliOrdinati);
		return articoliOrdinati;
	}

	public List<Articolo> getAllArticoliFromData(LocalDate data) {
		List<Articolo> trovati = new ArrayList<>();
		for (Articolo a : articoli) {
			if (data != null && a.getData().isEqual(data)) {
				System.out.println(data + " // " + a.getData());
				trovati.add(a);
			}
		}
		return trovati;
	}

	public List<Articolo> ricerca(String testo, Penna p, Mostrina m, LocalDate data) {

		Set<Articolo> rrSet = new HashSet<>();

		boolean flag;

		for (Articolo a : articoli) {
			flag = true;
			if (!testo.isEmpty() && !a.getTitolo().contains(testo)) {
				flag = false;
			}
			if (p != null && !a.getPenna().equals(p)) {
				flag = false;
			}
			if (m != null && !a.getMostrina().equals(m)) {
				flag = false;
			}
			if (data != null && data.isBefore(LocalDate.now()) && !a.getData().isEqual(data)) {
				flag = false;
			}
			if (flag == true) {
				rrSet.add(a);
			}
		}

		return orderByDate(rrSet);
	}

	public int getNumUpdatedArticols() {
		return numArtNew - numArtOld;
	}

	public List<Articolo> getArticoliSimili(Articolo a) {

		Map<Articolo, Integer> simili = new TreeMap<>();

		for (Articolo vicino : Graphs.neighborListOf(grafo, a)) {
			simili.put(vicino, (int) grafo.getEdgeWeight(grafo.getEdge(a, vicino)));
			grafo.getEdge(a, vicino);
		}

		Set<Map.Entry<Articolo, Integer>> set = simili.entrySet();
		List<Map.Entry<Articolo, Integer>> list = new ArrayList<Map.Entry<Articolo, Integer>>(set);

		Collections.sort(list, new Comparator<Map.Entry<Articolo, Integer>>() {
			@Override
			public int compare(Entry<Articolo, Integer> o1, Entry<Articolo, Integer> o2) {
				return o2.getValue().compareTo(o1.getValue());
			}
		});

		List<Articolo> similiOrdinati = new ArrayList<>();

		for (Map.Entry<Articolo, Integer> entry : list) {
			similiOrdinati.add(entry.getKey());
		}

		return similiOrdinati;
	}

}
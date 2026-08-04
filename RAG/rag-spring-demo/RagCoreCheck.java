import java.util.*;

// Validacao isolada dos algoritmos centrais do RAG (sem Spring),
// rodavel com: java RagCoreCheck.java
public class RagCoreCheck {

    static Map<String, Integer> vocab = new HashMap<>();
    static double[] idf = new double[0];
    static List<String> corpus;

    public static void main(String[] args) {
        corpus = List.of(
                "[0] O endpoint de health do Actuator informa se a aplicacao esta saudavel para balanceadores de carga.",
                "[1] Os profiles permitem configuracoes diferentes para producao. Para ativar um profile use spring.profiles.active.",
                "[2] O Spring Data JPA gera repositorios automaticamente ao estender JpaRepository, sem codigo manual.",
                "[3] Os starters como spring-boot-starter-web trazem Spring MVC, Jackson e Tomcat embutido de uma vez."
        );

        fit(corpus);
        double[][] vectors = new double[corpus.size()][];
        for (int i = 0; i < corpus.size(); i++) vectors[i] = embed(corpus.get(i));

        rank("Para que serve o endpoint de health do Actuator?", vectors);
        rank("Como ativar um profile de producao?", vectors);
        rank("Como criar um repositorio sem escrever implementacao?", vectors);

        System.out.println("Dimensao do vocabulario: " + vocab.size());
        System.out.println("OK: recuperacao por cosseno funcionando (ranking acima).");
    }

    static void rank(String question, double[][] vectors) {
        Integer[] order = new Integer[vectors.length];
        for (int i = 0; i < order.length; i++) order[i] = i;
        double[] q = embed(question);
        double[] scores = new double[vectors.length];
        for (int i = 0; i < vectors.length; i++) scores[i] = cosine(q, vectors[i]);
        Arrays.sort(order, (a, b) -> Double.compare(scores[b], scores[a]));
        System.out.println("\nPergunta: " + question);
        for (int rankPos = 0; rankPos < 2; rankPos++) {
            int idx = order[rankPos];
            System.out.printf("   %d. chunk #%d  score=%.3f%n", rankPos + 1, idx, scores[idx]);
        }
    }

    static void fit(List<String> docs) {
        Map<String, Integer> df = new HashMap<>();
        for (String doc : docs) {
            Set<String> terms = new HashSet<>(tokenize(doc));
            for (String t : terms) {
                vocab.computeIfAbsent(t, x -> vocab.size());
                df.merge(t, 1, Integer::sum);
            }
        }
        int n = docs.size();
        idf = new double[vocab.size()];
        for (var e : vocab.entrySet())
            idf[e.getValue()] = Math.log((double) (n + 1) / (df.getOrDefault(e.getKey(), 0) + 1)) + 1.0;
    }

    static double[] embed(String text) {
        double[] v = new double[vocab.size()];
        for (String t : tokenize(text)) {
            Integer idx = vocab.get(t);
            if (idx != null) v[idx] += 1.0;
        }
        for (int i = 0; i < v.length; i++) v[i] *= idf[i];
        double norm = 0; for (double x : v) norm += x * x; norm = Math.sqrt(norm);
        if (norm > 0) for (int i = 0; i < v.length; i++) v[i] /= norm;
        return v;
    }

    static double cosine(double[] a, double[] b) {
        int len = Math.min(a.length, b.length);
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < len; i++) { dot += a[i]*b[i]; na += a[i]*a[i]; nb += b[i]*b[i]; }
        if (na == 0 || nb == 0) return 0;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    static List<String> tokenize(String text) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (char c : text.toLowerCase().toCharArray()) {
            if (Character.isLetterOrDigit(c)) cur.append(c);
            else { if (cur.length() > 1) out.add(cur.toString()); cur.setLength(0); }
        }
        if (cur.length() > 1) out.add(cur.toString());
        return out;
    }
}

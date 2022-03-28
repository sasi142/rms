package core.daos;

import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.List;

public interface ElasticSearchDao {
    void flush();
    void flush(List<ImmutablePair<String, String>> messages);

    void add(String indexName, String json);
	String get(String indexName, Integer id);
}

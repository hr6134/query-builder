import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Утилита, помогающая избавиться от множества проверок при передаче параметров в запрос, из-за естественных
 * ограничений стандарта sql и мапирования ORM, которые не позволяют передавать в качестве параметров
 * пустые списки и null.
 *
 * По умолчанию, считаем, если список пуст или параметр равен null, часть запроса, использующая его,
 * опускается. Если хотим использовать более математическое соответствие, когда {@code value in ()} равно {@code false},
 * а {@code value not in ()} равно {@code true}, то выставляем значение переменной {@code behavior}
 * в {@code COMPARE_WITH_UNION}.
 *
 * Вот так выглядит следующий код с использованием {@link QueryBuilder}
 *
 * {@code   @PersistenceContext(unitName = "persistenceUnit")
            protected EntityManager em;

            StringBuilder sql = new StringBuilder(" select et from EfficiencyTemplate et where 1=1");
            if (contracts != null && !contracts.isEmpty()) {
                sql.append(" and et.contract in (:contracts)");
            }
            if (products != null && !products.isEmpty()) {
                sql.append(" and et.product in (:products)");
            }

            Query query = em.createQuery(sql.toString(), EfficiencyTemplate.class);
            if (contracts != null && !contracts.isEmpty()) {
                query.setParameter("contracts", contracts);
            }
            if (products != null && !products.isEmpty()) {
                query.setParameter("products", products);
            }

            List<EfficiencyTemplate> result = query.getResultList();
    }
 *
 * {@code @PersistenceContext(unitName = "persistenceUnit")
          protected EntityManager em;

          List<EfficiencyTemplate> result = new QueryBuilder(" select et from EfficiencyTemplate et where 1=1 ")
                .append(" and et.contract in (:contracts) ", "contracts", contracts)
                .append(" and et.product in (:products) ", "products", products)
                .buildQuery(em)
                .getResultList();
    }
 *
 * @author ltoshchev
 */

public class QueryBuilder {
    public static boolean OMIT_QUERY_PART = true;
    public static boolean COMPARE_WITH_UNION = false;

    private StringBuilder sql = new StringBuilder();
    private Map<String, Object> params = new HashMap<>();
    private boolean behavior = OMIT_QUERY_PART;

    public QueryBuilder() {

    }

    public QueryBuilder(String sql) {
        this.sql.append(sql);
    }

    /**
     * Добавляет часть запроса, в зависимости от того прошёл ли валидацию параметр или нет.
     * В случае выставления {@code behavior} в {@code COMPARE_WITH_UNION} на лету меняет оператор {@code [NOT] IN} на
     * 1 = 1 или 1 = 0
     *
     * @param sql часть sql запроса
     * @param key ключ параметра
     * @param param значение параметра
     * @return объект {@link QueryBuilder}, на котором вызывался метод
     */
    public QueryBuilder append(String sql, String key, Object param) {
        if (param != null) {
            if ((param instanceof Collection && !((Collection) param).isEmpty())
                    || !(param instanceof Collection)) {
                this.params.put(key, param);
                this.sql.append(sql);
            } else if (!behavior) {
                sql = sql.replaceAll(String.format("\\s[\\w.:]+\\s*(?<!not)\\s+in\\s+\\(?:%s\\)?", key), " 1 = 0 ");
                sql = sql.replaceAll(String.format("\\s[\\w.:]+\\s+not\\s+in\\s+\\(?:%s\\)?", key), " 1 = 1 ");
                this.params.put(key, param);
                this.sql.append(sql);
            }
        }
        return this;
    }

    /**
     * Меняет поведение {@link QueryBuilder}.
     * Можно выставить "опускать часть запроса при провале валидации" или "сравнивать значение с пустым множеством"
     *
     * @param behavior следует использовать константы {@code QueryBuilder.OMIT_QUERY_PART}
     *                 и {@code QueryBuilder.COMPARE_WITH_UNION}, объявленные в классе
     * @return объект {@link QueryBuilder}, на котором вызывался метод
     */
    public QueryBuilder setBehavior(boolean behavior) {
        this.behavior = behavior;
        return this;
    }

    /**
     * Используем, если всё же хотим иметь возможно передать {@code null} или пустой список
     *
     * @param sql часть sql запроса
     * @param key ключ параметра
     * @param param значение параметра
     * @return объект {@link QueryBuilder}, на котором вызывался метод
     */
    public QueryBuilder appendNullable(String sql, String key, Object param) {
        this.params.put(key, param);
        this.sql.append(sql);
        return this;
    }

    /**
     * Добавить часть запроса без параметров
     *
     * @param sql часть sql запроса
     * @return объект {@link QueryBuilder}, на котором вызывался метод
     */
    public QueryBuilder append(String sql) {
        this.sql.append(sql);
        return this;
    }

    public Query buildQuery(EntityManager em) {
        Query query = em.createQuery(this.sql.toString());
        for (Map.Entry<String, Object> e : params.entrySet()) {
            query.setParameter(e.getKey(), e.getValue());
        }

        return query;
    }

    public Query buildNativeQuery(EntityManager em) {
        Query query = em.createNativeQuery(this.sql.toString());
        for (Map.Entry<String, Object> e : params.entrySet()) {
            query.setParameter(e.getKey(), e.getValue());
        }

        return query;
    }
}

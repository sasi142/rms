package core.daos.impl;

import core.daos.VideoKycDao;
import core.entities.projections.VideoKyc;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.StoredProcedureQuery;
import java.util.List;
import java.util.Optional;

@Repository
public class VideoKycDaoImpl implements VideoKycDao {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<VideoKyc> getVideoKycStatusByGroupId(Long groupId) {
        StoredProcedureQuery spQuery = entityManager.createNamedStoredProcedureQuery("VideoKyc.GetStatusByGroupId");
        spQuery.setParameter("P_GroupId", groupId);
        List<VideoKyc> result = spQuery.getResultList();
        if (result.isEmpty()){
            return Optional.empty();
        }
        return Optional.of(result.get(0));
    }
}

package core.daos;

import core.entities.projections.VideoKyc;

import java.util.Optional;

public interface VideoKycDao {
    Optional<VideoKyc> getVideoKycStatusByGroupId(Long groupId);
}

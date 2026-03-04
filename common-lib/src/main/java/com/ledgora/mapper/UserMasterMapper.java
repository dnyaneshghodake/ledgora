package com.ledgora.mapper;

import com.ledgora.dto.UserMasterDTO;
import com.ledgora.model.UserMaster;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Mapper Class for UserMaster Entity and DTO
 *
 * <p>Converts between UserMaster entity and UserMasterDTO for API operations.
 */
@Component
public class UserMasterMapper {

    /**
     * Convert UserMaster Entity to DTO
     *
     * @param userMaster Entity object
     * @return DTO object
     */
    public UserMasterDTO toDTO(UserMaster userMaster) {
        if (userMaster == null) {
            return null;
        }

        return UserMasterDTO.builder()
                .usrCode1(userMaster.getUsrCode1())
                .usrCode2(userMaster.getUsrCode2())
                .usrCode3(userMaster.getUsrCode3())
                .usrShortName(userMaster.getUsrShortName())
                .usrName(userMaster.getUsrName())
                .emailId(userMaster.getEmailId())
                .extnNo(userMaster.getExtnNo())
                .usrBrCode(userMaster.getUsrBrCode())
                .lgnFrmOthBr(userMaster.getLgnFrmOthBr())
                .multiBrAccess(userMaster.getMultiBrAccess())
                .custAccessLvl(userMaster.getCustAccessLvl())
                .grpCd(userMaster.getGrpCd())
                .status(userMaster.getStatus())
                .isActive(userMaster.getIsActive())
                .customStatus(userMaster.getCustomStatus())
                .scrLockYN(userMaster.getScrLockYN())
                .scrLockAfterSecs(userMaster.getScrLockAfterSecs())
                .autoLogoutYN(userMaster.getAutoLogoutYN())
                .autoLogoutAfterSecs(userMaster.getAutoLogoutAfterSecs())
                .pwdChgForcedYN(userMaster.getPwdChgForcedYN())
                .pwdChgPeriodDays(userMaster.getPwdChgPeriodDays())
                .lastPwdChgDt(userMaster.getLastPwdChgDt())
                .nextPwdChgDt(userMaster.getNextPwdChgDt())
                .pwdNegativesMod(userMaster.getPwdNegativesMod())
                .minLiFreqForcedYN(userMaster.getMinLiFreqForcedYN())
                .minLiPeriodDays(userMaster.getMinLiPeriodDays())
                .maxBadLiPerDay(userMaster.getMaxBadLiPerDay())
                .maxBadLiPerInst(userMaster.getMaxBadLiPerInst())
                .badLoginsDt(userMaster.getBadLoginsDt())
                .noOfBadLogins(userMaster.getNoOfBadLogins())
                .lastSysLiDt(userMaster.getLastSysLiDt())
                .nextSysLiDt(userMaster.getNextSysLiDt())
                .accProfMnu(userMaster.getAccProfMnu())
                .accProfNodes(userMaster.getAccProfNodes())
                .accProfSysEnt(userMaster.getAccProfSysEnt())
                .maxRptFiles(userMaster.getMaxRptFiles())
                .activeInStn(userMaster.getActiveInStn())
                .activeSrNoInd(userMaster.getActiveSrNoInd())
                .custNo(userMaster.getCustNo())
                .alloReCount(userMaster.getAlloReCount())
                .alloUnReCount(userMaster.getAlloUnReCount())
                .build();
    }

    /**
     * Convert UserMasterDTO to Entity
     *
     * @param dto DTO object
     * @return Entity object
     */
    public UserMaster toEntity(UserMasterDTO dto) {
        if (dto == null) {
            return null;
        }

        UserMaster userMaster = new UserMaster();
        userMaster.setUsrCode1(dto.getUsrCode1());
        userMaster.setUsrCode2(dto.getUsrCode2());
        userMaster.setUsrCode3(dto.getUsrCode3());
        userMaster.setUsrShortName(dto.getUsrShortName());
        userMaster.setUsrName(dto.getUsrName());
        userMaster.setEmailId(dto.getEmailId());
        userMaster.setExtnNo(dto.getExtnNo());
        userMaster.setUsrBrCode(dto.getUsrBrCode());
        userMaster.setLgnFrmOthBr(dto.getLgnFrmOthBr());
        userMaster.setMultiBrAccess(dto.getMultiBrAccess());
        userMaster.setCustAccessLvl(dto.getCustAccessLvl());
        userMaster.setGrpCd(dto.getGrpCd());
        userMaster.setStatus(dto.getStatus());
        userMaster.setIsActive(dto.getIsActive());
        userMaster.setCustomStatus(dto.getCustomStatus());
        userMaster.setScrLockYN(dto.getScrLockYN());
        userMaster.setScrLockAfterSecs(dto.getScrLockAfterSecs());
        userMaster.setAutoLogoutYN(dto.getAutoLogoutYN());
        userMaster.setAutoLogoutAfterSecs(dto.getAutoLogoutAfterSecs());
        userMaster.setPwdChgForcedYN(dto.getPwdChgForcedYN());
        userMaster.setPwdChgPeriodDays(dto.getPwdChgPeriodDays());
        userMaster.setLastPwdChgDt(dto.getLastPwdChgDt());
        userMaster.setNextPwdChgDt(dto.getNextPwdChgDt());
        userMaster.setPwdNegativesMod(dto.getPwdNegativesMod());
        userMaster.setMinLiFreqForcedYN(dto.getMinLiFreqForcedYN());
        userMaster.setMinLiPeriodDays(dto.getMinLiPeriodDays());
        userMaster.setMaxBadLiPerDay(dto.getMaxBadLiPerDay());
        userMaster.setMaxBadLiPerInst(dto.getMaxBadLiPerInst());
        userMaster.setBadLoginsDt(dto.getBadLoginsDt());
        userMaster.setNoOfBadLogins(dto.getNoOfBadLogins());
        userMaster.setLastSysLiDt(dto.getLastSysLiDt());
        userMaster.setNextSysLiDt(dto.getNextSysLiDt());
        userMaster.setAccProfMnu(dto.getAccProfMnu());
        userMaster.setAccProfNodes(dto.getAccProfNodes());
        userMaster.setAccProfSysEnt(dto.getAccProfSysEnt());
        userMaster.setMaxRptFiles(dto.getMaxRptFiles());
        userMaster.setActiveInStn(dto.getActiveInStn());
        userMaster.setActiveSrNoInd(dto.getActiveSrNoInd());
        userMaster.setCustNo(dto.getCustNo());
        userMaster.setAlloReCount(dto.getAlloReCount());
        userMaster.setAlloUnReCount(dto.getAlloUnReCount());

        return userMaster;
    }

    /**
     * Convert list of UserMaster entities to DTO list
     *
     * @param userMasters List of entities
     * @return List of DTOs
     */
    public List<UserMasterDTO> toDTOList(List<UserMaster> userMasters) {
        if (userMasters == null) {
            return null;
        }
        return userMasters.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * Convert list of DTOs to entity list
     *
     * @param dtos List of DTOs
     * @return List of entities
     */
    public List<UserMaster> toEntityList(List<UserMasterDTO> dtos) {
        if (dtos == null) {
            return null;
        }
        return dtos.stream().map(this::toEntity).collect(Collectors.toList());
    }

    /**
     * Update entity from DTO (partial update)
     *
     * @param dto Source DTO
     * @param entity Target entity to update
     * @return Updated entity
     */
    public UserMaster updateEntityFromDTO(UserMasterDTO dto, UserMaster entity) {
        if (dto == null) {
            return entity;
        }

        if (dto.getUsrCode2() != null) {
            entity.setUsrCode2(dto.getUsrCode2());
        }
        if (dto.getUsrCode3() != null) {
            entity.setUsrCode3(dto.getUsrCode3());
        }
        if (dto.getUsrShortName() != null) {
            entity.setUsrShortName(dto.getUsrShortName());
        }
        if (dto.getUsrName() != null) {
            entity.setUsrName(dto.getUsrName());
        }
        if (dto.getEmailId() != null) {
            entity.setEmailId(dto.getEmailId());
        }
        if (dto.getExtnNo() != null) {
            entity.setExtnNo(dto.getExtnNo());
        }
        if (dto.getUsrBrCode() != null) {
            entity.setUsrBrCode(dto.getUsrBrCode());
        }
        if (dto.getLgnFrmOthBr() != null) {
            entity.setLgnFrmOthBr(dto.getLgnFrmOthBr());
        }
        if (dto.getMultiBrAccess() != null) {
            entity.setMultiBrAccess(dto.getMultiBrAccess());
        }
        if (dto.getCustAccessLvl() != null) {
            entity.setCustAccessLvl(dto.getCustAccessLvl());
        }
        if (dto.getGrpCd() != null) {
            entity.setGrpCd(dto.getGrpCd());
        }
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
        if (dto.getIsActive() != null) {
            entity.setIsActive(dto.getIsActive());
        }
        if (dto.getCustomStatus() != null) {
            entity.setCustomStatus(dto.getCustomStatus());
        }
        if (dto.getScrLockYN() != null) {
            entity.setScrLockYN(dto.getScrLockYN());
        }
        if (dto.getScrLockAfterSecs() != null) {
            entity.setScrLockAfterSecs(dto.getScrLockAfterSecs());
        }
        if (dto.getAutoLogoutYN() != null) {
            entity.setAutoLogoutYN(dto.getAutoLogoutYN());
        }
        if (dto.getAutoLogoutAfterSecs() != null) {
            entity.setAutoLogoutAfterSecs(dto.getAutoLogoutAfterSecs());
        }
        if (dto.getPwdChgForcedYN() != null) {
            entity.setPwdChgForcedYN(dto.getPwdChgForcedYN());
        }
        if (dto.getPwdChgPeriodDays() != null) {
            entity.setPwdChgPeriodDays(dto.getPwdChgPeriodDays());
        }
        if (dto.getLastPwdChgDt() != null) {
            entity.setLastPwdChgDt(dto.getLastPwdChgDt());
        }
        if (dto.getNextPwdChgDt() != null) {
            entity.setNextPwdChgDt(dto.getNextPwdChgDt());
        }
        if (dto.getPwdNegativesMod() != null) {
            entity.setPwdNegativesMod(dto.getPwdNegativesMod());
        }
        if (dto.getMinLiFreqForcedYN() != null) {
            entity.setMinLiFreqForcedYN(dto.getMinLiFreqForcedYN());
        }
        if (dto.getMinLiPeriodDays() != null) {
            entity.setMinLiPeriodDays(dto.getMinLiPeriodDays());
        }
        if (dto.getMaxBadLiPerDay() != null) {
            entity.setMaxBadLiPerDay(dto.getMaxBadLiPerDay());
        }
        if (dto.getMaxBadLiPerInst() != null) {
            entity.setMaxBadLiPerInst(dto.getMaxBadLiPerInst());
        }
        if (dto.getBadLoginsDt() != null) {
            entity.setBadLoginsDt(dto.getBadLoginsDt());
        }
        if (dto.getNoOfBadLogins() != null) {
            entity.setNoOfBadLogins(dto.getNoOfBadLogins());
        }
        if (dto.getLastSysLiDt() != null) {
            entity.setLastSysLiDt(dto.getLastSysLiDt());
        }
        if (dto.getNextSysLiDt() != null) {
            entity.setNextSysLiDt(dto.getNextSysLiDt());
        }
        if (dto.getAccProfMnu() != null) {
            entity.setAccProfMnu(dto.getAccProfMnu());
        }
        if (dto.getAccProfNodes() != null) {
            entity.setAccProfNodes(dto.getAccProfNodes());
        }
        if (dto.getAccProfSysEnt() != null) {
            entity.setAccProfSysEnt(dto.getAccProfSysEnt());
        }
        if (dto.getMaxRptFiles() != null) {
            entity.setMaxRptFiles(dto.getMaxRptFiles());
        }
        if (dto.getActiveInStn() != null) {
            entity.setActiveInStn(dto.getActiveInStn());
        }
        if (dto.getActiveSrNoInd() != null) {
            entity.setActiveSrNoInd(dto.getActiveSrNoInd());
        }
        if (dto.getCustNo() != null) {
            entity.setCustNo(dto.getCustNo());
        }
        if (dto.getAlloReCount() != null) {
            entity.setAlloReCount(dto.getAlloReCount());
        }
        if (dto.getAlloUnReCount() != null) {
            entity.setAlloUnReCount(dto.getAlloUnReCount());
        }

        return entity;
    }
}

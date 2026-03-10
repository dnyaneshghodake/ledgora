package com.ledgora.gl.service;

import com.ledgora.common.enums.GLAccountType;
import com.ledgora.gl.dto.GeneralLedgerDTO;
import com.ledgora.gl.entity.GeneralLedger;
import com.ledgora.gl.repository.GeneralLedgerRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GeneralLedgerService {

    private static final Logger log = LoggerFactory.getLogger(GeneralLedgerService.class);
    private final GeneralLedgerRepository glRepository;

    public GeneralLedgerService(GeneralLedgerRepository glRepository) {
        this.glRepository = glRepository;
    }

    @Transactional
    public GeneralLedger createGLAccount(GeneralLedgerDTO dto) {
        if (glRepository.existsByGlCode(dto.getGlCode())) {
            throw new RuntimeException("GL Code already exists: " + dto.getGlCode());
        }
        GeneralLedger parent = null;
        int level = 0;
        if (dto.getParentId() != null) {
            parent =
                    glRepository
                            .findById(dto.getParentId())
                            .orElseThrow(
                                    () ->
                                            new RuntimeException(
                                                    "Parent GL not found: " + dto.getParentId()));
            level = parent.getLevel() + 1;
        }
        GeneralLedger gl =
                GeneralLedger.builder()
                        .glCode(dto.getGlCode())
                        .glName(dto.getGlName())
                        .description(dto.getDescription())
                        .accountType(GLAccountType.valueOf(dto.getAccountType()))
                        .parent(parent)
                        .level(level)
                        .isActive(true)
                        .balance(BigDecimal.ZERO)
                        .normalBalance(
                                dto.getNormalBalance() != null ? dto.getNormalBalance() : "DEBIT")
                        .build();
        GeneralLedger saved = glRepository.save(gl);
        log.info("GL Account created: {} - {}", saved.getGlCode(), saved.getGlName());
        return saved;
    }

    public List<GeneralLedger> getAllGLAccounts() {
        return glRepository.findAll();
    }

    public Optional<GeneralLedger> getGLById(Long id) {
        return glRepository.findById(id);
    }

    public Optional<GeneralLedger> getGLByCode(String code) {
        return glRepository.findByGlCode(code);
    }

    public List<GeneralLedger> getRootAccounts() {
        return glRepository.findRootAccounts();
    }

    public List<GeneralLedger> getChildren(Long parentId) {
        return glRepository.findActiveChildren(parentId);
    }

    public List<GeneralLedger> getByType(GLAccountType type) {
        return glRepository.findByAccountType(type);
    }

    @Transactional
    public GeneralLedger updateGLAccount(Long id, GeneralLedgerDTO dto) {
        GeneralLedger gl =
                glRepository
                        .findById(id)
                        .orElseThrow(() -> new RuntimeException("GL Account not found: " + id));
        gl.setGlName(dto.getGlName());
        if (dto.getDescription() != null) {
            gl.setDescription(dto.getDescription());
        }
        if (dto.getNormalBalance() != null) {
            gl.setNormalBalance(dto.getNormalBalance());
        }
        if (dto.getIsActive() != null) {
            gl.setIsActive(dto.getIsActive());
        }
        return glRepository.save(gl);
    }

    @Transactional
    public void toggleGLStatus(Long id) {
        GeneralLedger gl =
                glRepository
                        .findById(id)
                        .orElseThrow(() -> new RuntimeException("GL Account not found: " + id));
        gl.setIsActive(!gl.getIsActive());
        glRepository.save(gl);
    }
}

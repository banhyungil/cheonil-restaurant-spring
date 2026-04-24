package com.ban.cheonil.store;

import com.ban.cheonil.store.dto.StoreCategoryRes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreCategoryService {

    private final StoreCategoryRepo storeCategoryRepo;

    public List<StoreCategoryRes> findAll() {
        return storeCategoryRepo.findAll().stream().map(StoreCategoryRes::from).toList();
    }
}

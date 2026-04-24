package com.ban.cheonil.store;

import com.ban.cheonil.store.dto.StoreRes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreService {

    private final StoreRepo storeRepo;

    public List<StoreRes> findAll() {
        return storeRepo.findAll().stream().map(StoreRes::from).toList();
    }
}

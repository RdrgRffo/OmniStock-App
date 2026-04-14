package com.omnistock.backend.service.product;

import com.omnistock.backend.dtos.SyncStatus;
import com.omnistock.backend.dtos.product.AggregatedProductDto;
import com.omnistock.backend.dtos.product.ProductDetailDto;
import com.omnistock.backend.dtos.product.ProductSearchRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Fachada de {@link IStockService}: consultas vía {@link StockQueryService},
 * sincronización de catálogo vía {@link StockCatalogSyncService}.
 */
@Service
public class StockServiceImpl implements IStockService {

    private final StockQueryService stockQueryService;
    private final StockCatalogSyncService stockCatalogSyncService;
    private final SyncStateService syncStateService;

    public StockServiceImpl(StockQueryService stockQueryService,
                            StockCatalogSyncService stockCatalogSyncService,
                            SyncStateService syncStateService) {
        this.stockQueryService = stockQueryService;
        this.stockCatalogSyncService = stockCatalogSyncService;
        this.syncStateService = syncStateService;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AggregatedProductDto> searchProducts(ProductSearchRequest request, Pageable pageable) {
        return stockQueryService.searchProducts(request, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AggregatedProductDto> findAllProducts(Pageable pageable) {
        return stockQueryService.findAllProducts(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProductDetailDto> findProductDetailsById(Integer id) {
        return stockQueryService.findProductDetailsById(id);
    }

    @Override
    @Async
    public void syncAllProducts() {
        stockCatalogSyncService.syncAllProducts();
    }

    @Override
    @Transactional(readOnly = true)
    public SyncStatus getSyncStatus() {
        return syncStateService.getStatus();
    }
}

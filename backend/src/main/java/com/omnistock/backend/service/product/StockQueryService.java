package com.omnistock.backend.service.product;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnistock.backend.dtos.DisponibilidadStatus;
import com.omnistock.backend.dtos.product.AggregatedProductDto;
import com.omnistock.backend.dtos.product.ProductDetailDto;
import com.omnistock.backend.dtos.product.ProductSearchRequest;
import com.omnistock.backend.dtos.product.SupplierOfferDto;
import com.omnistock.backend.entity.MasterProduct;
import com.omnistock.backend.entity.ProductSupplier;
import com.omnistock.backend.repository.MasterProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class StockQueryService {

    private static final Logger logger = LoggerFactory.getLogger(StockQueryService.class);
    private static final int MIN_STOCK_THRESHOLD = 5;

    private final MasterProductRepository masterProductRepository;
    private final ObjectMapper objectMapper;

    public StockQueryService(MasterProductRepository masterProductRepository, ObjectMapper objectMapper) {
        this.masterProductRepository = masterProductRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public Page<AggregatedProductDto> searchProducts(ProductSearchRequest request, Pageable pageable) {
        logger.info("Buscando productos con filtros: query='{}', brandFilter='{}', productCategory='{}', supplierId={}, specsFilter='{}'",
                request.query(), request.category(), request.productCategory(), request.supplierId(), request.specsFilter());

        Sort sort = pageable.getSort();
        boolean customSort = false;
        Sort.Direction direction = Sort.Direction.ASC;
        String sortProperty = "";

        if (sort.isSorted()) {
            for (Sort.Order order : sort) {
                if ("disponibilidad".equals(order.getProperty()) ||
                        "disponibilidadRank".equals(order.getProperty()) ||
                        "precioMinimo".equals(order.getProperty()) ||
                        "minCostPrice".equals(order.getProperty())) {
                    customSort = true;
                    direction = order.getDirection();
                    sortProperty = order.getProperty();
                    break;
                }
            }
        }

        if (customSort) {
            List<MasterProduct> allProducts = masterProductRepository.searchProductsList(
                    request.query(), request.category(), request.productCategory(), request.supplierId(), request.specsFilter()
            );

            List<AggregatedProductDto> allDtos = allProducts.stream()
                    .map(this::convertToAggregatedDto)
                    .collect(Collectors.toList());

            Comparator<AggregatedProductDto> comparator;
            if ("precioMinimo".equals(sortProperty) || "minCostPrice".equals(sortProperty)) {
                comparator = Comparator.comparing(AggregatedProductDto::minCostPrice, Comparator.nullsLast(Comparator.naturalOrder()));
            } else {
                comparator = Comparator.comparingInt(this::getAvailabilityScore);
            }

            if (direction == Sort.Direction.DESC) {
                comparator = comparator.reversed();
            }

            allDtos.sort(comparator);

            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), allDtos.size());
            List<AggregatedProductDto> pageContent;

            if (start >= allDtos.size()) {
                pageContent = List.of();
            } else {
                pageContent = allDtos.subList(start, end);
            }

            return new PageImpl<>(pageContent, pageable, allDtos.size());
        }

        Page<MasterProduct> productosPage = masterProductRepository.searchProducts(
                request.query(), request.category(), request.productCategory(), request.supplierId(), request.specsFilter(), pageable
        );
        logger.info("Resultados encontrados: {}", productosPage.getTotalElements());
        List<AggregatedProductDto> dtos = productosPage.getContent().stream()
                .map(this::convertToAggregatedDto)
                .collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, productosPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Page<AggregatedProductDto> findAllProducts(Pageable pageable) {
        return masterProductRepository.findAll(pageable).map(this::convertToAggregatedDto);
    }

    @Transactional(readOnly = true)
    public Optional<ProductDetailDto> findProductDetailsById(Integer id) {
        return masterProductRepository.findById(id).map(this::convertToDetailDto);
    }

    private int getAvailabilityScore(AggregatedProductDto dto) {
        if (dto.availability() == null) {
            return 0;
        }
        return switch (dto.availability()) {
            case DISPONIBLE -> 3;
            case BAJO_STOCK -> 2;
            case SIN_STOCK -> 1;
            case NO_INFO -> 0;
        };
    }

    private AggregatedProductDto convertToAggregatedDto(MasterProduct masterProduct) {
        List<ProductSupplier> offers = masterProduct.getProductSuppliers();

        BigDecimal minCost = offers.stream()
                .map(ProductSupplier::getPrice)
                .filter(p -> p != null && p.compareTo(BigDecimal.ZERO) > 0)
                .min(BigDecimal::compareTo)
                .orElse(null);

        BigDecimal minRetail = offers.stream()
                .map(ProductSupplier::getRetailPrice)
                .filter(p -> p != null && p.compareTo(BigDecimal.ZERO) > 0)
                .min(BigDecimal::compareTo)
                .orElse(null);

        int supplierCount = offers.size();

        int totalStock = offers.stream()
                .map(ProductSupplier::getStock)
                .filter(s -> s != null && s > 0)
                .mapToInt(Integer::intValue)
                .sum();

        DisponibilidadStatus availability;
        if (totalStock == 0) {
            availability = DisponibilidadStatus.SIN_STOCK;
        } else if (totalStock <= MIN_STOCK_THRESHOLD) {
            availability = DisponibilidadStatus.BAJO_STOCK;
        } else {
            availability = DisponibilidadStatus.DISPONIBLE;
        }

        Map<String, String> specs = Map.of();
        if (masterProduct.getTechSpecs() != null && !masterProduct.getTechSpecs().isBlank()) {
            try {
                specs = objectMapper.readValue(masterProduct.getTechSpecs(), new TypeReference<Map<String, String>>() {});
            } catch (IOException e) {
                logger.warn("No se pudieron deserializar las especificaciones de producto {}: {}", masterProduct.getId(), e.getMessage());
            }
        }

        return new AggregatedProductDto(
                masterProduct.getId(),
                masterProduct.getMpn(),
                masterProduct.getModel(),
                masterProduct.getBrand(),
                masterProduct.getCategory(),
                minCost,
                minRetail,
                availability,
                supplierCount,
                specs
        );
    }

    private ProductDetailDto convertToDetailDto(MasterProduct masterProduct) {
        Map<String, String> specs = Map.of();
        if (masterProduct.getTechSpecs() != null && !masterProduct.getTechSpecs().isBlank()) {
            try {
                specs = objectMapper.readValue(masterProduct.getTechSpecs(), new TypeReference<Map<String, String>>() {});
            } catch (IOException e) {
                logger.warn("No se pudieron deserializar las especificaciones de producto {}: {}", masterProduct.getId(), e.getMessage());
            }
        }

        List<SupplierOfferDto> offers = masterProduct.getProductSuppliers().stream()
                .map(this::toSupplierOfferDto)
                .collect(Collectors.toList());

        return new ProductDetailDto(
                masterProduct.getId(),
                masterProduct.getMpn(),
                masterProduct.getModel(),
                masterProduct.getBrand(),
                masterProduct.getCategory(),
                specs,
                offers
        );
    }

    private SupplierOfferDto toSupplierOfferDto(ProductSupplier ps) {
        String condition = ps.getProductCondition() != null ? ps.getProductCondition().name() : null;

        String stockStatus;
        Integer stock = ps.getStock();
        if (stock == null || stock == 0) {
            stockStatus = "SIN_STOCK";
        } else if (stock <= MIN_STOCK_THRESHOLD) {
            stockStatus = "BAJO_STOCK";
        } else {
            stockStatus = "DISPONIBLE";
        }

        return new SupplierOfferDto(
                ps.getSupplier() != null ? ps.getSupplier().getId() : null,
                ps.getSupplier() != null ? ps.getSupplier().getName() : null,
                ps.getPrice(),
                ps.getRetailPrice(),
                ps.getStock(),
                ps.getLastUpdated(),
                stockStatus,
                ps.getEan(),
                ps.getMoq(),
                condition
        );
    }
}

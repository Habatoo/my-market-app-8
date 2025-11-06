package io.github.habatoo.servicies;

import io.github.habatoo.mappers.BaseMapper;
import org.springframework.data.jpa.repository.JpaRepository;

public abstract class AbstractService<E, D> {

    protected final JpaRepository<E, Long> repository;
    protected final BaseMapper<E, D> mapper;

    public AbstractService(JpaRepository<E, Long> repository, BaseMapper<E, D> mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    /**
     * Получение DTO по id
     */
    public D getById(Long id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElse(null);
    }

    /**
     * Сохранение DTO по id
     */
    public void save(E entity) {
        repository.save(entity);
    }
}

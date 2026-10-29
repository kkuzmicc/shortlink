package com.shortlink.links;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class LinkRepository implements PanacheRepositoryBase<LinkEntity, UUID> {
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public Link insert(String code, String targetUrl, String title) {
        LinkEntity entity = new LinkEntity();

        entity.code = code;
        entity.targetUrl = targetUrl;
        entity.title = title;

        persistAndFlush(entity);

        return map(entity);
    }

    @Transactional
    public List<Link> getList(int limit, int offset) {
        return find("order by createdAt desc, id desc")
                .range(offset, (int) Math.min(Integer.MAX_VALUE, (long) offset + limit - 1))
                .list().stream().map(this::map).toList();
    }

    private Link map(LinkEntity entity) {
        return new Link(entity.id, entity.code, entity.targetUrl, entity.title,
                entity.status, entity.createdAt);
    }

    public record Link(UUID id, String code, String targetUrl, String title, String status, Instant createdAt) {}
}

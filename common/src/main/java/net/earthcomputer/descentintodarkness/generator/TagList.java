package net.earthcomputer.descentintodarkness.generator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.earthcomputer.descentintodarkness.style.DIDCodecs;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public record TagList(List<String> tags, boolean tagsInverted) {
    public static final MapCodec<TagList> CODEC = RecordCodecBuilder.<SerdeProxy>mapCodec(instance -> instance.group(
        DIDCodecs.singleableList(Codec.STRING).optionalFieldOf("tags").forGetter(SerdeProxy::tags),
        Codec.BOOL.optionalFieldOf("tags_inverted").forGetter(SerdeProxy::tagsInverted)
    ).apply(instance, SerdeProxy::new)).xmap(TagList::new, SerdeProxy::new);

    private TagList(SerdeProxy proxy) {
        this(proxy.tags.orElseGet(List::of), proxy.tagsInverted.orElseGet(proxy.tags::isEmpty));
    }

    public boolean matches(Collection<String> tags) {
        return tagsInverted ? this.tags.stream().noneMatch(tags::contains) : this.tags.stream().anyMatch(tags::contains);
    }

    private record SerdeProxy(Optional<List<String>> tags, Optional<Boolean> tagsInverted) {
        SerdeProxy(TagList tagList) {
            this(tagList.tags.isEmpty() ? Optional.empty() : Optional.of(tagList.tags), tagList.tags.isEmpty() == tagList.tagsInverted ? Optional.empty() : Optional.of(tagList.tagsInverted));
        }
    }
}

package io.github.mattidragon.tlaapi.impl.rei;

import io.github.mattidragon.tlaapi.api.recipe.TlaRecipe;
import io.github.mattidragon.tlaapi.impl.Untyped;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class TlaDisplay implements Display {
    private final CategoryIdentifier<?> category;
    private final TlaRecipe recipe;

    static final MapCodec<TlaDisplay> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Identifier.CODEC.xmap(CategoryIdentifier::of, id -> Identifier.of(id.getNamespace(), id.getPath())).fieldOf("category").forGetter(o -> Untyped.cast(o.category)),
            TlaRecipe.CODEC.fieldOf("recipe").forGetter(o -> o.recipe)
    ).apply(i, TlaDisplay::new));
    static final PacketCodec<RegistryByteBuf, TlaDisplay> PACKET_CODEC = PacketCodec.tuple(
            Identifier.PACKET_CODEC.xmap(CategoryIdentifier::of, id -> Identifier.of(id.getNamespace(), id.getPath())), o -> o.category,
            TlaRecipe.PACKET_CODEC, o -> o.recipe,
            TlaDisplay::new
    );
    static final Identifier ID = Identifier.of("tla", "display");
    static final DisplaySerializer<TlaDisplay> SERIALIZER = DisplaySerializer.of(MAP_CODEC, PACKET_CODEC);

    public TlaDisplay(CategoryIdentifier<?> category, TlaRecipe recipe) {
        this.category = category;
        this.recipe = recipe;
    }

    public static TlaDisplay of(TlaRecipe recipe) {
        return new TlaDisplay(ReiImplementation.INSTANCE.getCategoryIdentifier(recipe.getCategory()), recipe);
    }

    @Override
    public DisplaySerializer<? extends Display> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public Optional<Identifier> getDisplayLocation() {
        return Optional.ofNullable(recipe.getId()).map(RegistryKey::getValue);
    }

    @Override
    public List<EntryIngredient> getInputEntries() {
        return recipe.getInputs().stream().map(ReiUtil::convertIngredient).toList();
    }

    @Override
    public List<EntryIngredient> getOutputEntries() {
        return recipe.getOutputs().stream().map(ReiUtil::convertStack).map(EntryIngredient::of).toList();
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return category;
    }

    public TlaRecipe getRecipe() {
        return recipe;
    }
}

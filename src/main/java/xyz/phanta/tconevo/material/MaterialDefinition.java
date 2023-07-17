package xyz.phanta.tconevo.material;

import io.github.phantamanta44.libnine.util.helper.OreDictUtils;
import net.minecraftforge.fluids.Fluid;
import slimeknights.tconstruct.library.TinkerRegistry;
import slimeknights.tconstruct.library.materials.Material;
import slimeknights.tconstruct.library.traits.ITrait;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import xyz.phanta.tconevo.TconEvoMod;
import xyz.phanta.tconevo.util.LazyAccum;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class MaterialDefinition {

    private static final List<String> METAL_PREFIXES = Arrays.asList(
            "ingot", "nugget", "dust", "ore", "oreNether", "denseore", "orePoor", "oreNugget", "block", "plate", "gear");

    private static final List<MaterialDefinition> materialDefs = new ArrayList<>();

    public static void register(Material material,
                                MaterialForm form,
                                String oreName,
                                List<RegCondition> conditions,
                                Map<PartType, LazyAccum<ITrait>> traits) {
        materialDefs.add(new MaterialDefinition(material, form, oreName, conditions, traits));
    }

    public static void initMaterialProperties() {
        for (MaterialDefinition defn : materialDefs) {
            try {
                defn.initProperties();
            } catch (Exception e) {
                TconEvoMod.LOGGER.error("Encountered exception while initializing material {}", defn.material.identifier);
                TconEvoMod.LOGGER.error("Stack trace:", e);
            }
        }
    }

    public static void activate() {
        for (MaterialDefinition defn : materialDefs) {
            try {
                defn.tryActivate();
            } catch (Exception e) {
                TconEvoMod.LOGGER.error("Encountered exception while activating material {}", defn.material.identifier);
                TconEvoMod.LOGGER.error("Stack trace:", e);
            }
        }
    }

    private final Material material;
    private final MaterialForm form;
    private final String oreName;

    private final List<RegCondition> conditions;
    private final Map<PartType, LazyAccum<ITrait>> traits;

    private MaterialDefinition(Material material,
                               MaterialForm form,
                               String oreName,
                               List<RegCondition> conditions,
                               Map<PartType, LazyAccum<ITrait>> traits) {
        this.material = material;
        this.form = form;
        this.oreName = oreName;
        this.conditions = conditions;
        this.traits = traits;
    }

    private void initProperties() {
        if (form == MaterialForm.METAL) {
            material.addCommonItems(oreName);
        } else {
            for (MaterialForm.Entry entry : form.entries) {
                material.addItem(entry.prefix + oreName, 1, entry.value);
            }
        }
        for (Map.Entry<PartType, LazyAccum<ITrait>> traitEntry : traits.entrySet()) {
            for (String typeKey : traitEntry.getKey().typeKeys) {
                for (ITrait trait : traitEntry.getValue().collect()) {
                    if (!material.hasTrait(trait.getIdentifier(), typeKey)) { // some part types have overlapping keys
                        material.addTrait(trait, typeKey);
                    }
                }
            }
        }
    }

    private void tryActivate() {
        if (MaterialBuilder.isNotWhitelisted(material.identifier)) {
            for (RegCondition condition : conditions) {
                if (!condition.isSatisfied()) {
                    return;
                }
            }
        }
        (form == MaterialForm.METAL ? METAL_PREFIXES.stream() : form.entries.stream().map(e -> e.prefix))
                .map(prefix -> prefix + oreName)
                .filter(OreDictUtils::exists)
                .findFirst()
                .ifPresent(material::setRepresentativeItem);
        Fluid fluid = material.getFluid();
        if (fluid != null) {
            if (form == MaterialForm.METAL) {
                TinkerSmeltery.registerOredictMeltingCasting(material.getFluid(), oreName);
            } else {
                for (MaterialForm.Entry entry : form.entries) {
                    String oreKey = entry.prefix + oreName;
                    TinkerRegistry.registerMelting(oreKey, fluid, entry.value);
                    if (entry.castType != null) {
                        entry.castType.registerCasting(oreKey, fluid, entry.value);
                    }
                }
            }
            TinkerSmeltery.registerToolpartMeltingCasting(material);
        }
        material.setVisible();
    }

}

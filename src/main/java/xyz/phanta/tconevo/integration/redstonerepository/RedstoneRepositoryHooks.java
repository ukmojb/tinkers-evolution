package xyz.phanta.tconevo.integration.redstonerepository;

import net.minecraft.item.ItemStack;
import xyz.phanta.tconevo.integration.IntegrationHooks;

import java.util.Optional;

public interface RedstoneRepositoryHooks {

    String MOD_ID = "redstonerepository";

    @IntegrationHooks.Inject(MOD_ID)
    RedstoneRepositoryHooks INSTANCE = new Noop();

    Optional<ItemStack> getItemGelidCapacitor();

    class Noop implements RedstoneRepositoryHooks {

        @Override
        public Optional<ItemStack> getItemGelidCapacitor() {
            return Optional.empty();
        }

    }

}

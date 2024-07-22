package me.declipsonator.globaldatapack.mixins;

import com.google.gson.JsonObject;
import me.declipsonator.globaldatapack.GlobalDatapack;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.*;
import net.minecraft.server.SaveLoader;
import net.minecraft.world.level.storage.LevelStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Mixin(MinecraftClient.class)
public class MinecraftMixin {
    @ModifyVariable(method = "startIntegratedServer", at = @At("HEAD"), index = 2, argsOnly = true)
    private ResourcePackManager startIntegratedServer(ResourcePackManager value, LevelStorage.Session session, ResourcePackManager dataPackManager, SaveLoader saveLoader, boolean newWorld) {
        if(!GlobalDatapack.config.has(session.getDirectoryName())) GlobalDatapack.config.add(session.getDirectoryName(), new JsonObject());

        // Get the other datapacks
        ArrayList<String> datapacks = new ArrayList<>(value.getIds());

        // Add the available datapacks and load them
        ArrayList<ResourcePackProvider> providers = new ArrayList<>(((ResourcePackManagerAccessor) value).getProviders());
        providers.add(new FileResourcePackProvider(GlobalDatapack.availablePackFolder, ResourceType.SERVER_DATA,  ResourcePackSource.NONE, session.getLevelStorage().getSymlinkFinder()));
        ((ResourcePackManagerAccessor) value).setProviders(new HashSet<>(providers));
        saveLoader.dataPackContents().refresh();
        value.scanPacks();

        // Get the updated array
        ArrayList<String> newDatapacks = new ArrayList<>(value.getIds());

        // Remove other datapacks, so you only have the "available" ones
        newDatapacks.removeAll(datapacks);



        // Disable them all

        // They're not entirely disabled until they're added to this thing
        ArrayList<String> disabled = new ArrayList<>(saveLoader.saveProperties().getDataConfiguration().dataPacks().getDisabled());

        for(String pack : newDatapacks) {
            if(!disabled.contains(pack) &&
                    (!GlobalDatapack.config.get(session.getDirectoryName()).getAsJsonObject().has(pack))
                    || !GlobalDatapack.config.get(session.getDirectoryName()).getAsJsonObject().get(pack).getAsBoolean()) {
                value.disable(pack);
                disabled.add(pack);
                if(!GlobalDatapack.config.get(session.getDirectoryName()).getAsJsonObject().has(pack))
                    GlobalDatapack.config.get(session.getDirectoryName()).getAsJsonObject().addProperty(pack, false);
            } else {
                value.enable(pack);
            }
        }
        ((DataPackSettingsAccessor) saveLoader.saveProperties().getDataConfiguration().dataPacks()).setDisabled(disabled);


        // Why not
        value.scanPacks();

        return value;
    }


}

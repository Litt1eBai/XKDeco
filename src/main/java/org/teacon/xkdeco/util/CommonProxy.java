package org.teacon.xkdeco.util;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import org.teacon.xkdeco.XKDeco;
import org.teacon.xkdeco.block.behavior.BlockBehaviorRegistry;
import org.teacon.xkdeco.block.loader.KBlockDefinition;
import org.teacon.xkdeco.block.loader.KCreativeTab;
import org.teacon.xkdeco.block.loader.KMaterial;
import org.teacon.xkdeco.block.loader.LoaderExtraCodecs;
import org.teacon.xkdeco.block.loader.LoaderExtraRegistries;
import org.teacon.xkdeco.block.setting.BlockRenderSettings;
import org.teacon.xkdeco.block.setting.KBlockComponent;
import org.teacon.xkdeco.block.setting.KBlockSettings;
import org.teacon.xkdeco.data.XKDDataGen;
import org.teacon.xkdeco.duck.XKBlockProperties;
import org.teacon.xkdeco.entity.CushionEntity;
import org.teacon.xkdeco.init.XKDecoObjects;

import javax.annotation.ParametersAreNonnullByDefault;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.FolderRepositorySource;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.Tags;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.ModLoader;
import net.minecraftforge.fml.ModLoadingStage;
import net.minecraftforge.fml.ModLoadingWarning;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.forgespi.locating.IModFile;
import net.minecraftforge.registries.NewRegistryEvent;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryBuilder;
import net.minecraftforge.resource.DelegatingPackResources;
import net.minecraftforge.resource.PathPackResources;
import net.minecraftforge.resource.ResourcePackLoader;
import snownee.kiwi.Kiwi;
import snownee.kiwi.KiwiTabBuilder;
import snownee.kiwi.datagen.GameObjectLookup;
import snownee.kiwi.loader.Platform;

@Mod(XKDeco.ID)
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class CommonProxy {
	public CommonProxy() {
		var modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

		XKDecoObjects.ENTITIES.register(modEventBus);
		XKDecoObjects.BLOCKS.register(modEventBus);
		XKDecoObjects.ITEMS.register(modEventBus);
		XKDecoObjects.BLOCK_ENTITY.register(modEventBus);

		modEventBus.addListener(EventPriority.LOWEST, XKDecoObjects::addMimicWallBlocks);
		modEventBus.addListener(EventPriority.LOWEST, XKDecoObjects::addMimicWallItems);
		modEventBus.addListener(EventPriority.LOWEST, XKDecoObjects::addMimicWallBlockEntity);

		modEventBus.addListener((GatherDataEvent event) -> {
			FabricDataGenerator dataGenerator = FabricDataGenerator.create(XKDeco.ID, event);
			new XKDDataGen().onInitializeDataGenerator(dataGenerator);
		});
		modEventBus.addListener(EventPriority.LOW, (RegisterEvent event) -> {
			if (!Registries.BLOCK.equals(event.getRegistryKey())) {
				return;
			}
			// set an empty settings to all blocks, to make them water-loggable correctly
			// seems unnecessary anymore
			GameObjectLookup.all(Registries.BLOCK, XKDeco.ID).forEach(block -> {
				KBlockSettings settings = KBlockSettings.of(block);
				if (settings == null) {
					((XKBlockProperties) block.properties).xkdeco$setSettings(KBlockSettings.EMPTY);
				} else {
					BlockBehaviorRegistry behaviorRegistry = BlockBehaviorRegistry.getInstance();
					for (KBlockComponent component : settings.components.values()) {
						behaviorRegistry.setContext(block);
						component.addBehaviors(behaviorRegistry);
					}
					behaviorRegistry.setContext(null);
				}
			});

			initLoader();
		});
		modEventBus.addListener((NewRegistryEvent event) -> {
			ResourceLocation registryKey = new ResourceLocation(Kiwi.ID, "block_component");
			event.create(
					new RegistryBuilder<>().setName(registryKey).disableOverrides().disableSaving().hasTags(),
					$ -> {
						//noinspection unchecked
						LoaderExtraRegistries.BLOCK_COMPONENT = (Registry<KBlockComponent.Type<?>>) BuiltInRegistries.REGISTRY.get(
								registryKey);
						Kiwi.registerRegistry($, KBlockComponent.Type.class);
					});
		});
		modEventBus.addListener(XKDecoObjects::addMimicWallsToTab);

		if (Platform.isPhysicalClient()) {
			ClientProxy.init();
		}

		var forgeEventBus = MinecraftForge.EVENT_BUS;

		forgeEventBus.addListener(XKDecoObjects::addMimicWallTags);

		forgeEventBus.addListener(CushionEntity::onRightClickBlock);
		forgeEventBus.addListener(CushionEntity::onBreakBlock);

		forgeEventBus.addListener((PlayerInteractEvent.RightClickBlock event) -> {
			InteractionResult result = BlockBehaviorRegistry.getInstance().onUseBlock(
					event.getEntity(),
					event.getLevel(),
					event.getHand(),
					event.getHitVec());
			if (result.consumesAction()) {
				event.setCanceled(true);
				event.setCancellationResult(result);
			}
		});
	}

	public static void initLoader() {
		Path packDirectory = FMLPaths.GAMEDIR.get().resolve("kiwipacks");
		//noinspection ResultOfMethodCallIgnored
		packDirectory.toFile().mkdirs();
		FolderRepositorySource folderRepositorySource = new FolderRepositorySource(
				packDirectory,
				PackType.CLIENT_RESOURCES,
				PackSource.DEFAULT);
		PackRepository packRepository = new PackRepository(folderRepositorySource);
		ResourcePackLoader.loadResourcePacks(packRepository, CommonProxy::buildPackFinder);
		packRepository.reload();
		packRepository.setSelected(packRepository.getAvailableIds());
		ResourceManager resourceManager = new MultiPackResourceManager(PackType.CLIENT_RESOURCES, packRepository.openAllSelected());
		var materials = JsonLoader.load(resourceManager, "kiwi/material", KMaterial.DIRECT_CODEC);
		var blocks = JsonLoader.load(resourceManager, "kiwi/block", KBlockDefinition.codec(LoaderExtraCodecs.simpleByNameCodec(materials)));
		if (Platform.isPhysicalClient()) {
			BlockRenderSettings.init(blocks);
		}
		var tabs = JsonLoader.load(resourceManager, "kiwi/creative_tab", KCreativeTab.CODEC);
		tabs.entrySet().stream().sorted(Comparator.comparingInt($ -> $.getValue()
				.order())).forEach(entry -> {
			KCreativeTab value = entry.getValue();
			CreativeModeTab tab = new KiwiTabBuilder(entry.getKey())
					.icon(() -> BuiltInRegistries.ITEM.getOptional(value.icon()).orElse(Items.BARRIER).getDefaultInstance())
					.displayItems((params, output) -> {
						output.acceptAll(value.contents()
								.stream()
								.map(BuiltInRegistries.ITEM::get)
								.filter(Objects::nonNull)
								.map(Item::getDefaultInstance)
								.toList());
					})
					.build();
			Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, entry.getKey(), tab);
		});
	}

	private static RepositorySource buildPackFinder(Map<IModFile, ? extends PathPackResources> modResourcePacks) {
		return packAcceptor -> clientPackFinder(modResourcePacks, packAcceptor);
	}

	private static void clientPackFinder(Map<IModFile, ? extends PathPackResources> modResourcePacks, Consumer<Pack> packAcceptor) {
		var hiddenPacks = new ArrayList<PathPackResources>();
		for (Map.Entry<IModFile, ? extends PathPackResources> e : modResourcePacks.entrySet()) {
			IModInfo mod = e.getKey().getModInfos().get(0);
			final String name = "mod:" + mod.getModId();
			final Pack modPack = Pack.readMetaAndCreate(
					name,
					Component.literal(e.getValue().packId()),
					false,
					id -> e.getValue(),
					PackType.CLIENT_RESOURCES,
					Pack.Position.BOTTOM,
					PackSource.DEFAULT);
			if (modPack == null) {
				// Vanilla only logs an error, instead of propagating, so handle null and warn that something went wrong
				ModLoader.get().addWarning(new ModLoadingWarning(mod, ModLoadingStage.ERROR, "fml.modloading.brokenresources", e.getKey()));
				continue;
			}
			XKDeco.LOGGER.debug("Generating PackInfo named {} for mod file {}", name, e.getKey().getFilePath());
			if (mod.getOwningFile().showAsResourcePack()) {
				packAcceptor.accept(modPack);
			} else {
				hiddenPacks.add(e.getValue());
			}
		}

		// Create a resource pack merging all mod resources that should be hidden
		final Pack modResourcesPack = Pack.readMetaAndCreate("mod_resources", Component.literal("Mod Resources"), true,
				id -> new DelegatingPackResources(id, false, new PackMetadataSection(
						Component.translatable("fml.resources.modresources", hiddenPacks.size()),
						SharedConstants.getCurrentVersion().getPackVersion(PackType.CLIENT_RESOURCES)), hiddenPacks),
				PackType.CLIENT_RESOURCES, Pack.Position.BOTTOM, PackSource.DEFAULT);
		packAcceptor.accept(modResourcesPack);
	}

	public static boolean isColorlessGlass(BlockState blockState) {
		return blockState.is(Tags.Blocks.GLASS_COLORLESS);
	}

	public static boolean isLadder(BlockState blockState, LevelReader world, BlockPos pos) {
		return blockState.isLadder(world, pos, null);
	}
}

package org.teacon.xkdeco.util;

import java.io.BufferedReader;
import java.util.Map;

import org.teacon.xkdeco.XKDeco;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

public class JsonLoader {
	private static final Gson GSON = new GsonBuilder().create();

	public static <T> Map<ResourceLocation, T> load(ResourceManager resourceManager, String directory, Codec<T> codec) {
		FileToIdConverter fileToIdConverter = FileToIdConverter.json(directory);
		Map<ResourceLocation, T> results = Maps.newHashMap();
		for (Map.Entry<ResourceLocation, Resource> entry : fileToIdConverter.listMatchingResources(resourceManager).entrySet()) {
			ResourceLocation id = fileToIdConverter.fileToId(entry.getKey());
			try (BufferedReader reader = entry.getValue().openAsReader()) {
				JsonObject json = GSON.fromJson(reader, JsonObject.class);
//				XKDeco.LOGGER.info(entry.getKey() + " " + json);
				DataResult<T> result = codec.parse(JsonOps.INSTANCE, json);
				if (result.error().isPresent()) {
					XKDeco.LOGGER.error("Failed to parse " + id + ": " + result.error().get());
					continue;
				}
				results.put(id, result.result().orElseThrow());
			} catch (Exception e) {
				throw new IllegalStateException("Failed to load " + id, e);
			}
		}
		return results;
	}
}

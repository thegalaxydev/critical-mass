package com.criticalmass.client;

import com.criticalmass.CriticalMass;
import com.criticalmass.registry.CMBlocks;
import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTModel;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTSpriteShifter;
import com.simibubi.create.foundation.block.connected.HorizontalCTBehaviour;
import com.simibubi.create.foundation.block.connected.RotatedPillarCTBehaviour;
import com.simibubi.create.foundation.block.connected.SimpleCTBehaviour;

public final class CMSpriteShifts {
    public static final CTSpriteShiftEntry REACTOR_VESSEL_CASING = omni("reactor_vessel_casing");
    public static final CTSpriteShiftEntry SHIELDED_GLASS = omni("shielded_glass");

    public static final CTSpriteShiftEntry CHARTRINE_CAP = CTSpriteShifter.getCT(AllCTTypes.OMNIDIRECTIONAL, CriticalMass.asResource("block/chartrine_cut_cap"), CriticalMass.asResource("block/chartrine_cut_cap_connected"));
    public static final CTSpriteShiftEntry CHARTRINE_LAYERED = CTSpriteShifter.getCT(AllCTTypes.HORIZONTAL_KRYPPERS, CriticalMass.asResource("block/chartrine_cut_layered"), CriticalMass.asResource("block/chartrine_cut_layered_connected"));
    public static final CTSpriteShiftEntry CHARTRINE_PILLAR = CTSpriteShifter.getCT(AllCTTypes.RECTANGLE, CriticalMass.asResource("block/chartrine_cut_pillar"), CriticalMass.asResource("block/chartrine_cut_pillar_connected"));

    private CMSpriteShifts() {
    }

    private static CTSpriteShiftEntry omni(String name) {
        return CTSpriteShifter.getCT(AllCTTypes.OMNIDIRECTIONAL, CriticalMass.asResource("block/" + name), CriticalMass.asResource("block/" + name + "_connected"));
    }

    public static void init() {
        SimpleCTBehaviour casing = new SimpleCTBehaviour(REACTOR_VESSEL_CASING);
        CreateClient.MODEL_SWAPPER.getCustomBlockModels().register(CMBlocks.REACTOR_VESSEL_CASING.getId(), model -> new CTModel(model, casing));
        SimpleCTBehaviour glass = new SimpleCTBehaviour(SHIELDED_GLASS);
        CreateClient.MODEL_SWAPPER.getCustomBlockModels().register(CMBlocks.SHIELDED_GLASS.getId(), model -> new CTModel(model, glass));
        HorizontalCTBehaviour layered = new HorizontalCTBehaviour(CHARTRINE_LAYERED, CHARTRINE_CAP);
        CreateClient.MODEL_SWAPPER.getCustomBlockModels().register(CMBlocks.LAYERED_CHARTRINE.getId(), model -> new CTModel(model, layered));
        RotatedPillarCTBehaviour pillar = new RotatedPillarCTBehaviour(CHARTRINE_PILLAR, CHARTRINE_CAP);
        CreateClient.MODEL_SWAPPER.getCustomBlockModels().register(CMBlocks.CHARTRINE_PILLAR.getId(), model -> new CTModel(model, pillar));
    }
}

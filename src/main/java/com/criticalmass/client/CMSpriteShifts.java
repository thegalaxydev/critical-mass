package com.criticalmass.client;

import com.criticalmass.CriticalMass;
import com.criticalmass.registry.CMBlocks;
import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTModel;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTSpriteShifter;
import com.simibubi.create.foundation.block.connected.SimpleCTBehaviour;

public final class CMSpriteShifts {
    public static final CTSpriteShiftEntry REACTOR_VESSEL_CASING = omni("reactor_vessel_casing");

    private CMSpriteShifts() {
    }

    private static CTSpriteShiftEntry omni(String name) {
        return CTSpriteShifter.getCT(AllCTTypes.OMNIDIRECTIONAL, CriticalMass.asResource("block/" + name), CriticalMass.asResource("block/" + name + "_connected"));
    }

    public static void init() {
        SimpleCTBehaviour casing = new SimpleCTBehaviour(REACTOR_VESSEL_CASING);
        CreateClient.MODEL_SWAPPER.getCustomBlockModels().register(CMBlocks.REACTOR_VESSEL_CASING.getId(), model -> new CTModel(model, casing));
    }
}

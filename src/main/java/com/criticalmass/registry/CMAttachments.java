package com.criticalmass.registry;

import com.criticalmass.content.thermal.PipeCoolant;
import com.criticalmass.CriticalMass;
import com.criticalmass.content.radiation.RadiationData;
import java.util.function.Supplier;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class CMAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, CriticalMass.MODID);

    public static final Supplier<AttachmentType<RadiationData>> RADIATION = ATTACHMENTS.register("radiation",
            () -> AttachmentType.serializable(RadiationData::new).copyOnDeath().build());
    public static final Supplier<AttachmentType<PipeCoolant>> PIPE_COOLANT = ATTACHMENTS.register("pipe_coolant",
            () -> AttachmentType.serializable(PipeCoolant::new).build());

    private CMAttachments() {
    }
}

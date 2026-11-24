package com.criticalmass.client.entity;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.radiation.IrradiatedCreeper;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

public class IrradiatedCreeperModel extends HierarchicalModel<IrradiatedCreeper> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(CriticalMass.asResource("irradiated_creeper"), "main");
    public static final ModelLayerLocation ARMOR = new ModelLayerLocation(CriticalMass.asResource("irradiated_creeper"), "armor");
    private static final float DEGREE = Mth.PI / 180F;
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart secondHead;
    private final ModelPart spareLeg;
    private final ModelPart flap;
    private final ModelPart belly;
    private final ModelPart hump;
    private final ModelPart eyeGrowth;
    private final ModelPart[] boils;
    private final ModelPart frontRight;
    private final ModelPart frontLeft;
    private final ModelPart hindRight;
    private final ModelPart hindLeft;
    private float swelling;

    public IrradiatedCreeperModel(ModelPart root) {
        this.root = root;
        body = root.getChild("body");
        head = body.getChild("head");
        jaw = head.getChild("jaw");
        eyeGrowth = head.getChild("eye_growth");
        flap = head.getChild("flap");
        secondHead = body.getChild("second_head");
        spareLeg = body.getChild("spare_leg");
        belly = body.getChild("belly");
        hump = body.getChild("hump");
        boils = new ModelPart[] {hump.getChild("boil_a"), body.getChild("boil_b"), belly.getChild("boil_c")};
        frontRight = root.getChild("leg_front_right");
        frontLeft = root.getChild("leg_front_left");
        hindRight = root.getChild("leg_hind_right");
        hindLeft = root.getChild("leg_hind_left");
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void prepareMobModel(IrradiatedCreeper entity, float limbSwing, float limbSwingAmount, float partialTick) {
        swelling = Mth.clamp(entity.getSwelling(partialTick), 0F, 1F);
    }

    private static float twitch(float age, float every, float offset) {
        float phase = (age + offset) % every;
        return phase < 6F ? Mth.sin(phase / 6F * Mth.PI) : 0F;
    }

    @Override
    public void setupAnim(IrradiatedCreeper entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        float own = entity.getId() * 7.3F;
        float age = ageInTicks + own;
        float stride = limbSwing * 0.6F;
        float amount = Math.min(1F, limbSwingAmount);
        float breath = Mth.sin(age * 0.07F);

        frontRight.xRot += Mth.cos(stride) * 0.85F * amount;
        frontRight.zRot += Math.max(0F, Mth.sin(stride)) * 0.12F * amount;
        frontLeft.xRot += Mth.cos(stride + Mth.PI) * 1.35F * amount;
        hindRight.xRot += Mth.cos(stride + Mth.PI + 0.6F) * 1.6F * amount;
        hindLeft.xRot += Mth.cos(stride + 0.3F) * 1.15F * amount;

        body.zRot += Mth.sin(stride) * 0.13F * amount + breath * 0.012F;
        body.xRot += amount * 0.1F + breath * 0.015F + swelling * 0.12F;
        body.y += Math.abs(Mth.cos(stride)) * 0.7F * amount;

        head.yRot += netHeadYaw * DEGREE;
        head.xRot += headPitch * DEGREE + Mth.sin(age * 0.05F) * 0.04F - swelling * 0.25F;
        head.zRot += Mth.sin(age * 0.043F) * 0.07F - Mth.sin(stride) * 0.1F * amount + twitch(age, 137F, 0F) * 0.3F;
        jaw.xRot += 0.06F + breath * 0.06F + Math.abs(Mth.sin(stride)) * 0.18F * amount + swelling * 0.6F;
        flap.zRot += Mth.sin(age * 0.11F) * 0.14F + Mth.sin(stride) * 0.35F * amount;
        flap.xRot += Mth.cos(stride) * 0.3F * amount;

        secondHead.yRot += Mth.sin(age * 0.19F) * 0.16F + twitch(age, 61F, 20F) * 0.55F;
        secondHead.xRot += Mth.sin(age * 0.27F + 1F) * 0.1F - twitch(age, 97F, 40F) * 0.4F;
        secondHead.zRot += Mth.sin(stride + 1F) * 0.12F * amount;

        spareLeg.zRot += Mth.sin(age * 0.09F) * 0.08F + Mth.cos(stride) * 0.3F * amount + twitch(age, 173F, 60F) * 0.5F;
        spareLeg.xRot += Mth.sin(stride) * 0.55F * amount;

        float gut = 1F + breath * 0.035F + swelling * 0.35F;
        belly.xScale = gut;
        belly.yScale = 1F + breath * 0.02F + swelling * 0.2F;
        belly.zScale = gut + swelling * 0.25F;
        hump.yScale = 1F - breath * 0.03F + swelling * 0.2F;
        float bulge = 1F + Mth.sin(age * 0.31F) * 0.06F + swelling * 0.5F;
        eyeGrowth.xScale = bulge;
        eyeGrowth.yScale = bulge;
        eyeGrowth.zScale = 1F + swelling * 1.5F;
        for (int i = 0; i < boils.length; i++) {
            float pulse = 1F + Mth.sin(age * 0.23F + i * 2.1F) * 0.1F + swelling * 0.45F;
            boils[i].xScale = pulse;
            boils[i].yScale = pulse;
            boils[i].zScale = pulse;
        }
    }
}

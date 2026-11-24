package com.criticalmass.client.entity;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public final class IrradiatedCreeperParts {
    private IrradiatedCreeperParts() {
    }

    public static LayerDefinition create(CubeDeformation grow) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16).addBox(-4F, -12F, -2F, 8F, 12F, 4F, grow), PartPose.offsetAndRotation(0F, 18F, 0F, 0.1396F, 0F, 0.0698F));
        PartDefinition belly = body.addOrReplaceChild("belly", CubeListBuilder.create().texOffs(0, 36).addBox(-4F, -3F, -3F, 7F, 6F, 3F, grow), PartPose.offsetAndRotation(1F, -4F, -2F, 0F, 0F, 0.0524F));
        belly.addOrReplaceChild("boil_c", CubeListBuilder.create().texOffs(58, 10).addBox(-1F, -1F, -1F, 2F, 2F, 1F, grow), PartPose.offset(1F, 0F, -3F));
        PartDefinition hump = body.addOrReplaceChild("hump", CubeListBuilder.create().texOffs(20, 36).addBox(-3F, -3F, 0F, 6F, 5F, 3F, grow), PartPose.offsetAndRotation(-2F, -10F, 2F, -0.2094F, 0F, -0.1745F));
        hump.addOrReplaceChild("boil_a", CubeListBuilder.create().texOffs(52, 4).addBox(-1F, -2F, -1F, 2F, 2F, 2F, grow), PartPose.offset(-1F, -3F, 1.5F));
        hump.addOrReplaceChild("spur_a", CubeListBuilder.create().texOffs(24, 45).addBox(-1F, -3F, -1F, 1F, 3F, 1F, grow), PartPose.offsetAndRotation(2F, -3F, 2F, -0.4363F, 0F, 0.4887F));
        hump.addOrReplaceChild("spur_b", CubeListBuilder.create().texOffs(24, 45).addBox(0F, -3F, -1F, 1F, 3F, 1F, grow), PartPose.offsetAndRotation(-3F, -2F, 2.5F, -0.6981F, 0F, -0.4189F));
        body.addOrReplaceChild("boil_b", CubeListBuilder.create().texOffs(48, 10).addBox(0F, -2F, -1F, 2F, 3F, 3F, grow), PartPose.offset(4F, -7F, 0F));
        body.addOrReplaceChild("ribs", CubeListBuilder.create().texOffs(0, 45).addBox(-1F, -2F, -2F, 1F, 1F, 4F, grow).texOffs(0, 45).addBox(-1F, 0F, -2F, 1F, 1F, 4F, grow).texOffs(0, 45).addBox(-1F, 2F, -2F, 1F, 1F, 4F, grow), PartPose.offset(-4F, -8F, 0F));
        body.addOrReplaceChild("spare_leg", CubeListBuilder.create().texOffs(52, 27).addBox(-3F, 0F, -2F, 3F, 5F, 3F, grow), PartPose.offsetAndRotation(-4F, -3F, 1F, -0.1745F, 0F, 0.4363F));
        body.addOrReplaceChild("second_head", CubeListBuilder.create().texOffs(32, 0).addBox(-1F, -5F, -3F, 5F, 5F, 5F, grow), PartPose.offsetAndRotation(4F, -12F, 0F, 0F, -0.3491F, 0.5236F));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4F, -8F, -4F, 8F, 8F, 8F, grow), PartPose.offsetAndRotation(0F, -12F, 0F, -0.1047F, 0F, -0.1745F));
        head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(40, 36).addBox(-3F, 0F, -3F, 6F, 2F, 3F, grow), PartPose.offsetAndRotation(0F, -0.5F, -1F, 0.3142F, 0F, 0F));
        head.addOrReplaceChild("eye_growth", CubeListBuilder.create().texOffs(52, 0).addBox(-1F, -1F, -1F, 3F, 3F, 1F, grow), PartPose.offset(2F, -5F, -4F));
        PartDefinition scalp = head.addOrReplaceChild("scalp", CubeListBuilder.create().texOffs(32, 10).addBox(-2F, -2F, -2F, 4F, 2F, 4F, grow), PartPose.offsetAndRotation(-2F, -8F, 2F, 0.1047F, 0F, -0.1396F));
        scalp.addOrReplaceChild("spur_c", CubeListBuilder.create().texOffs(24, 45).addBox(-1F, -3F, 0F, 1F, 3F, 1F, grow), PartPose.offsetAndRotation(1F, -2F, 1F, -0.5236F, 0F, 0.2793F));
        head.addOrReplaceChild("flap", CubeListBuilder.create().texOffs(18, 45).addBox(-1F, 0F, -1F, 1F, 4F, 2F, grow), PartPose.offsetAndRotation(-4F, -1F, -1F, 0F, 0F, 0.1047F));
        root.addOrReplaceChild("leg_front_right", CubeListBuilder.create().texOffs(40, 16).addBox(-3F, 0F, -3F, 5F, 6F, 5F, grow), PartPose.offsetAndRotation(-2F, 18F, -4F, 0F, 0.2793F, 0F));
        root.addOrReplaceChild("leg_front_left", CubeListBuilder.create().texOffs(0, 16).addBox(-2F, 0F, -2F, 4F, 6F, 4F, grow), PartPose.offsetAndRotation(2F, 18F, -4F, 0F, -0.2094F, 0F));
        root.addOrReplaceChild("leg_hind_right", CubeListBuilder.create().texOffs(40, 27).addBox(-2F, 0F, -1F, 3F, 6F, 3F, grow), PartPose.offsetAndRotation(-2F, 18F, 4F, 0F, -0.1745F, 0F));
        root.addOrReplaceChild("leg_hind_left", CubeListBuilder.create().texOffs(0, 26).addBox(-2F, 0F, -2F, 4F, 6F, 4F, grow).texOffs(10, 45).addBox(2F, 1F, -1F, 2F, 2F, 2F, grow), PartPose.offsetAndRotation(2F, 18F, 4F, 0F, 0.2443F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }
}

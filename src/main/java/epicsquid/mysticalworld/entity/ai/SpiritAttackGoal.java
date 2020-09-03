package epicsquid.mysticalworld.entity.ai;

import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.TargetGoal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.pathfinding.Path;
import net.minecraft.util.EntityPredicates;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

import java.util.EnumSet;

public class SpiritAttackGoal extends TargetGoal {
  protected final CreatureEntity attacker;
  protected int attackTick;
  private final double speedTowardsTarget;
  private final boolean longMemory;
  private Path path;
  private int delayCounter;
  private double targetX;
  private double targetY;
  private double targetZ;
  private long field_220720_k;
  private int failedPathFindingPenalty = 0;
  private boolean canPenalize = false;

  public SpiritAttackGoal(CreatureEntity creature, double speedIn, boolean useLongMemory) {
    super(creature, false, false);
    this.attacker = creature;
    this.speedTowardsTarget = speedIn;
    this.longMemory = useLongMemory;
    this.setMutexFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
  }

  @Override
  public boolean shouldExecute() {
    long i = this.attacker.world.getGameTime();
    if (i - this.field_220720_k < 20L) {
      return false;
    } else {
      this.field_220720_k = i;
      LivingEntity livingentity = this.attacker.getAttackTarget();
      if (livingentity == null) {
        return false;
      } else if (!livingentity.isAlive()) {
        return false;
      } else {
        if (canPenalize) {
          if (--this.delayCounter <= 0) {
            this.path = this.attacker.getNavigator().getPathToEntity(livingentity, 0);
            this.delayCounter = 4 + this.attacker.getRNG().nextInt(7);
            return this.path != null;
          } else {
            return true;
          }
        }
        this.path = this.attacker.getNavigator().getPathToEntity(livingentity, 0);
        if (this.path != null) {
          return true;
        } else {
          return 5 >= this.attacker.getDistanceSq(livingentity.getPosX(), livingentity.getPosY(), livingentity.getPosZ());
        }
      }
    }
  }

  @Override
  public boolean shouldContinueExecuting() {
    LivingEntity livingentity = this.attacker.getAttackTarget();
    if (livingentity == null) {
      return false;
    } else if (!livingentity.isAlive()) {
      return false;
    } else if (!this.longMemory) {
      return !this.attacker.getNavigator().noPath();
    } else if (!this.attacker.isWithinHomeDistanceFromPosition(new BlockPos(livingentity))) {
      return false;
    } else {
      return !(livingentity instanceof PlayerEntity) || !livingentity.isSpectator() && !((PlayerEntity) livingentity).isCreative();
    }
  }

  @Override
  public void startExecuting() {
    this.attacker.getNavigator().setPath(this.path, this.speedTowardsTarget);
    this.attacker.setAggroed(true);
    this.delayCounter = 0;
  }

  @Override
  public void resetTask() {
    LivingEntity livingentity = this.attacker.getAttackTarget();
    if (!EntityPredicates.CAN_AI_TARGET.test(livingentity)) {
      this.attacker.setAttackTarget(null);
    }

    this.attacker.setAggroed(false);
    this.attacker.getNavigator().clearPath();
  }

  @Override
  public void tick() {
    LivingEntity livingentity = this.attacker.getAttackTarget();
    this.attacker.getLookController().setLookPositionWithEntity(livingentity, 30.0F, 30.0F);
    double d0 = this.attacker.getDistanceSq(livingentity.getPosX(), livingentity.getPosY(), livingentity.getPosZ());
    --this.delayCounter;
    if ((this.longMemory || this.attacker.getEntitySenses().canSee(livingentity)) && this.delayCounter <= 0 && (this.targetX == 0.0D && this.targetY == 0.0D && this.targetZ == 0.0D || livingentity.getDistanceSq(this.targetX, this.targetY, this.targetZ) >= 1.0D || this.attacker.getRNG().nextFloat() < 0.05F)) {
      this.targetX = livingentity.getPosX();
      this.targetY = livingentity.getPosY();
      this.targetZ = livingentity.getPosZ();
      this.delayCounter = 4 + this.attacker.getRNG().nextInt(7);

      if (d0 > 1024.0D) {
        this.delayCounter += 10;
      } else if (d0 > 256.0D) {
        this.delayCounter += 5;
      }

      if (!this.attacker.getNavigator().tryMoveToEntityLiving(livingentity, this.speedTowardsTarget)) {
        this.delayCounter += 15;
      }
    }

    this.attackTick = Math.max(this.attackTick - 1, 0);
    this.checkAndPerformAttack(livingentity, d0);
  }

  protected void checkAndPerformAttack(LivingEntity enemy, double distToEnemySqr) {
    double d0 = this.getAttackReachSqr(enemy);
    if (distToEnemySqr <= d0 && this.attackTick <= 0) {
      this.attackTick = 20;
      this.attacker.swingArm(Hand.MAIN_HAND);
      this.attacker.attackEntityAsMob(enemy);
      this.attacker.remove();
    }

  }

  protected double getAttackReachSqr(LivingEntity attackTarget) {
    return (double) (this.attacker.getWidth() * 2.0F * this.attacker.getWidth() * 2.0F + attackTarget.getWidth());
  }
}

package game.ui

import gamelogic.entities.Body
import scala.reflect.ClassTag
import typings.pixiJs.mod.Sprite
import game.Camera
import gamelogic.gamestate.GameState
import gamelogic.entities.Entity

import scala.collection.mutable
import typings.pixiJs.mod.Texture

final class EntitySpriteContainer[EntityType <: Body](
    val container: typings.pixiJs.mod.Container,
    texture: typings.pixiJs.PIXI.Texture,
    camera: Camera,
    textureScale: Double = 1.0
)(
    implicit classTag: ClassTag[EntityType]
) {

  private val entitySpritesMap: mutable.Map[Entity.Id, Sprite] = mutable.Map.empty

  private def drawEntities(entities: List[EntityType], time: Long): Unit = {
    // First removing dead ones

    val currentIds = entities.map(_.id).toSet

    entitySpritesMap
      .filterNot { case (entityId, _) => currentIds.contains(entityId) }
      .foreach {
        case (entityId, sprite) =>
          sprite.destroy()
          entitySpritesMap -= entityId
      }

    entities.foreach { entity =>
      val sprite = entitySpritesMap.getOrElse(entity.id, {
        val s = new Sprite(texture)
        s.anchor.set(0.5, 0.5)
        entitySpritesMap += (entity.id -> s)
        container.addChild(s)
        s
      })

      sprite.rotation = -entity.rotation
      camera.viewportManager(sprite, entity.currentPosition(time), entity.shape.boundingBox)
      sprite.scale.set(sprite.scale.x * textureScale, sprite.scale.y * textureScale)
    }
  }

  def update(gameState: GameState, time: Long): Unit =
    drawEntities(gameState.allTEntities[EntityType].values.toList, time)

  def maybeSpriteById(entityId: Entity.Id): Option[Sprite] = entitySpritesMap.get(entityId)

}

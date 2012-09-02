package jp.ne.internavi.sync.hcp.dist
package thrift.conversions

import com.twitter.util.Time
import com.twitter.conversions.time._
import jp.ne.internavi.sync.hcp.dist
import jp.ne.internavi.sync.hcp.chord

object Hint {
  class RichShardingHint(hint: dist.Hint) {
    def toThrift = new thrift.hint(
      hint.id,
      hint.node_to,
      hint.table_name,
      hint.method_name, 
      hint.query, 
      hint.col_name, 
      hint.bool_value,
      hint.validity,
      hint.ins)
  }
  implicit def shardingHintToRichShardingHint(hint: dist.Hint) = new RichShardingHint(hint)

  class RichThriftHint(hint: thrift.hint) {
    def fromThrift = new dist.Hint(
      hint.id,
      hint.node_to,
      hint.table_name,
      hint.method_name, 
      hint.query, 
      hint.col_name, 
      hint.bool_value,
      hint.validity,
      hint.ins
    )
  }
  implicit def thfitHintToRichThriftHint(hint: thrift.hint) = new RichThriftHint(hint)
  
}

object Wall {
  class RichShardingWall(wall: dist.Wall) {
    def toThrift = new thrift.delivery_wall_messages(
    	wall.id,
        wall.global_id,
    	wall.product_own_id,
      	wall.category_id,
      	wall.service_id,
      	wall.view,      
      	wall.provider_id,     	
      	wall.seen,
      	wall.validity,
      	wall.delivery_start,
      	wall.table_name,
      	wall.id1,
      	wall.id2,
      	wall.id3,
      	wall.content_tokyo,
      	wall.content_itrf,
      	wall.ins)
  }
  implicit def shardingWallToRichShardingWall(wall: dist.Wall) = new RichShardingWall(wall)
  
  class RichThriftWall(wall: thrift.delivery_wall_messages) {
    def fromThrift = new dist.Wall(
    	wall.id,
	wall.global_id,
    	wall.product_own_id,
      	wall.category_id,
      	wall.service_id,
      	wall.view,      	
      	wall.provider_id,      	
      	wall.seen,
      	wall.validity,
      	wall.delivery_start,
      	wall.table_name,
      	wall.id1,
      	wall.id2,
      	wall.id3,
      	wall.content_tokyo,
      	wall.content_itrf,
      	wall.ins)       
  }
  implicit def thriftWallToRichThriftWall(wall: thrift.delivery_wall_messages) = new RichThriftWall(wall)
 }

package jp.ne.internavi.sync.hcp.dist

import com.twitter.util.Time
import java.nio.ByteBuffer

@serializable
case class Hint(id: Long,
		node_to: String,
		table_name: String,
		method_name: String, 
		query: String, 
		col_name: String, 
		bool_value: Boolean,
		validity: Boolean,
		ins: String)

@serializable
case class Image(source_id: Long, 
		 global_id: Long,
		 global_id_check_flg: Boolean,
		 validity: Boolean,
		 source_type: String, 
		 source: ByteBuffer)

@serializable
case class Wall(id: Long, 
		global_id: Long, 
		product_own_id: Long, 
		category_id: Int, 
		service_id: Int,
		view: Int,
		provider_id: Int,
		seen: Boolean, 
		validity: Boolean, 
		delivery_start: Int, 
		table_name: String, 
		id1: Long, 
		id2: Long,
		id3: Long,
		content_tokyo: String,
		content_itrf: String,
		ins: String)

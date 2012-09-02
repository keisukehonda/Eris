namespace java jp.ne.internavi.sync.hcp.dist.thrift

enum ConsistencyLevel {
     PRIME = 1,     
     NO_REPLICA = 2
}

struct hint {
    1: i64 id
    2: string node_to
    3: string table_name
    4: string method_name
    5: string query
    6: string col_name
    7: bool bool_value    
    8: bool validity
    9: string ins
}

struct delivery_wall_messages {       
    1: i64 id
    2: i64 global_id
    3: i64 product_own_id
    4: i32 category_id
    5: i32 service_id
    6: i32 view
    7: i32 provider_id    
    8: bool seen 			
    9: bool validity 		
   10: i32 delivery_start 	
   11: string table_name 	
   12: i64 id1 				
   13: i64 id2 				
   14: i64 id3 				
   15: string content_tokyo 
   16: string content_itrf
   17: string ins   
}

struct delivery_images {
    1: i64 source_id 
    2: i64 global_id
    3: bool global_id_check_flg
    4: bool validity
    5: string source_type 
    6: binary source 
}

struct statistics {
       	1: string uptime
	2: string time
	3: double version
	4: string table
}

struct column_bool{
   1: bool value
}

struct column_long{
   1: i64 value
}

exception RowzException {
  1: string description
}

service Rowz {
  //p2p network command
  //client I/F
  string join(1:string hostname, 2:i32 port) throws (1: RowzException ex)
  string remove(1:string hostname, 2:i32 port) throws (1: RowzException ex)
  string down(1:string hostname, 2:i32 port) throws (1: RowzException ex)
  string leave(1:string hostname, 2:i32 port) throws (1: RowzException ex)
  string delmark() throws (1: RowzException ex)
  string shutdown() throws (1: RowzException ex)
  statistics stats() throws(1: RowzException ex)

  //internal api
  i32 leaveAction(1:string hostname, 2:i32 port) throws (1: RowzException ex)
  i32 activateLeavePicker(1:string uri) throws (1: RowzException ex)
  
  //not yet
  string genID(1: string key) throws(1: RowzException ex)
  string get(1: string id) throws(1: RowzException ex)  
  list<string> getHosts(1: string id) throws(1: RowzException ex)

  //table manipulation api  
  list<hint> get_hint(1: required i32 offset, 
  	              2: required i32 limit,
                      3: required string query,
		      4: required ConsistencyLevel consistency_level)
		      throws(1: RowzException ex)

  i64 put_hint(1: required string key,
      	       2: required hint hint,
	       3: required ConsistencyLevel consistency_level) 
	       throws(1: RowzException ex)
	       
  i64 del_hint(1: required string key,
               2: required ConsistencyLevel consistency_level)
	       throws(1: RowzException ex)

  list<column_long> get_ids(1: required string tablename,
  		    	    2: required i32 offset, 
			    3: required i32 limit,
			    4: required string query,
			    5: required ConsistencyLevel consistency_level)
			    throws(1: RowzException ex)

  i64 put_delivery_wall_messages(1: required string key,
                                 2: required delivery_wall_messages wall,
                                 3: required ConsistencyLevel consistency_level) throws(1: RowzException ex)

  list<delivery_wall_messages> get_delivery_wall_messages(1: required string key,
                                                          2: required string query,
                                                          3: required ConsistencyLevel consistency_level) 
							  throws(1: RowzException ex)

  i64 delete_delivery_wall_messages(1: required string key,
                                    2: required string query,
                                    3: required ConsistencyLevel consistency_level) throws(1: RowzException ex)
							  
  i64 get_column_count(1: required string key,
                       2: required string tablename,
                       3: required string colname,
                       4: required string query,
                       5: required ConsistencyLevel consistency_level) throws(1: RowzException ex)

  i64 set_column_bool(1: required string key,
                      2: required string tablename,
                      3: required string colname,
                      4: required column_bool c1,
                      5: required string query,
                      6: required ConsistencyLevel consistency_level) throws(1: RowzException ex)

  list<column_long> get_column_long(1: required string key,
                                    2: required string tablename,
                                    3: required string colname,
                                    4: required string query,
                                    5: required ConsistencyLevel consistency_level) throws(1: RowzException ex)

  i64 put_delivery_images(1: required string key,
                          2: required delivery_images image,
                          3: required ConsistencyLevel consistency_level) throws(1: RowzException ex)
                          
  list<delivery_images> get_delivery_images(1: required string key,
                                            2: required string query,
                                            3: required ConsistencyLevel consistency_level) throws(1: RowzException ex)
                                                  
  i64 delete_delivery_images(1: required string key,
                             2: required string query,
                             3: required ConsistencyLevel consistency_level) throws(1: RowzException ex)
   
}

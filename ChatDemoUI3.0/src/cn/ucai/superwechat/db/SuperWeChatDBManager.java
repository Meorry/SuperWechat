package cn.ucai.superwechat.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.hyphenate.easeui.domain.EaseUser;
import com.hyphenate.easeui.domain.User;
import com.hyphenate.easeui.utils.EaseCommonUtils;
import com.hyphenate.util.HanziToPinyin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import cn.ucai.superwechat.Constant;
import cn.ucai.superwechat.SuperWeChatApplication;
import cn.ucai.superwechat.domain.InviteMessage;
import cn.ucai.superwechat.domain.RobotUser;

public class SuperWeChatDBManager {
    static private SuperWeChatDBManager dbMgr = new SuperWeChatDBManager();
    private DbOpenHelper dbHelper;
    
    private SuperWeChatDBManager(){
        dbHelper = DbOpenHelper.getInstance(SuperWeChatApplication.getInstance().getApplicationContext());
    }
    
    public static synchronized SuperWeChatDBManager getInstance(){
        if(dbMgr == null){
            dbMgr = new SuperWeChatDBManager();
        }
        return dbMgr;
    }
    
    /**
     * save contact list
     * 
     * @param contactList
     */
    synchronized public void saveContactList(List<EaseUser> contactList) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        if (db.isOpen()) {
            db.delete(UserDao.TABLE_NAME, null, null);
            for (EaseUser user : contactList) {
                ContentValues values = new ContentValues();
                values.put(UserDao.COLUMN_NAME_ID, user.getUsername());
                if(user.getNick() != null)
                    values.put(UserDao.COLUMN_NAME_NICK, user.getNick());
                if(user.getAvatar() != null)
                    values.put(UserDao.COLUMN_NAME_AVATAR, user.getAvatar());
                db.replace(UserDao.TABLE_NAME, null, values);
            }
        }
    }

    /**
     * get contact list
     * 
     * @return
     */
    synchronized public Map<String, EaseUser> getContactList() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Map<String, EaseUser> users = new Hashtable<String, EaseUser>();
        if (db.isOpen()) {
            Cursor cursor = db.rawQuery("select * from " + UserDao.TABLE_NAME /* + " desc" */, null);
            while (cursor.moveToNext()) {
                String username = cursor.getString(cursor.getColumnIndex(UserDao.COLUMN_NAME_ID));
                String nick = cursor.getString(cursor.getColumnIndex(UserDao.COLUMN_NAME_NICK));
                String avatar = cursor.getString(cursor.getColumnIndex(UserDao.COLUMN_NAME_AVATAR));
                EaseUser user = new EaseUser(username);
                user.setNick(nick);
                user.setAvatar(avatar);
                if (username.equals(Constant.NEW_FRIENDS_USERNAME) || username.equals(Constant.GROUP_USERNAME)
                        || username.equals(Constant.CHAT_ROOM)|| username.equals(Constant.CHAT_ROBOT)) {
                        user.setInitialLetter("");
                } else {
                    EaseCommonUtils.setUserInitialLetter(user);
                }
                users.put(username, user);
            }
            cursor.close();
        }
        return users;
    }
    
    /**
     * delete a contact
     * @param username
     */
    synchronized public void deleteContact(String username){
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        if(db.isOpen()){
            db.delete(UserDao.TABLE_NAME, UserDao.COLUMN_NAME_ID + " = ?", new String[]{username});
        }
    }
    
    /**
     * save a contact
     * @param user
     */
    synchronized public void saveContact(EaseUser user){
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(UserDao.COLUMN_NAME_ID, user.getUsername());
        if(user.getNick() != null)
            values.put(UserDao.COLUMN_NAME_NICK, user.getNick());
        if(user.getAvatar() != null)
            values.put(UserDao.COLUMN_NAME_AVATAR, user.getAvatar());
        if(db.isOpen()){
            db.replace(UserDao.TABLE_NAME, null, values);
        }
    }
    
    public void setDisabledGroups(List<String> groups){
        setList(UserDao.COLUMN_NAME_DISABLED_GROUPS, groups);
    }
    
    public List<String>  getDisabledGroups(){       
        return getList(UserDao.COLUMN_NAME_DISABLED_GROUPS);
    }
    
    public void setDisabledIds(List<String> ids){
        setList(UserDao.COLUMN_NAME_DISABLED_IDS, ids);
    }
    
    public List<String> getDisabledIds(){
        return getList(UserDao.COLUMN_NAME_DISABLED_IDS);
    }
    
    synchronized private void setList(String column, List<String> strList){
        StringBuilder strBuilder = new StringBuilder();
        
        for(String hxid:strList){
            strBuilder.append(hxid).append("$");
        }
        
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        if (db.isOpen()) {
            ContentValues values = new ContentValues();
            values.put(column, strBuilder.toString());

            db.update(UserDao.PREF_TABLE_NAME, values, null,null);
        }
    }
    
    synchronized private List<String> getList(String column){
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("select " + column + " from " + UserDao.PREF_TABLE_NAME,null);
        if (!cursor.moveToFirst()) {
            cursor.close();
            return null;
        }

        String strVal = cursor.getString(0);
        if (strVal == null || strVal.equals("")) {
            return null;
        }
        
        cursor.close();
        
        String[] array = strVal.split("$");
        
        if(array.length > 0){
            List<String> list = new ArrayList<String>();
            Collections.addAll(list, array);
            return list;
        }
        
        return null;
    }
    
    /**
     * save a message
     * @param message
     * @return  return cursor of the message
     */
    public synchronized Integer saveMessage(InviteMessage message){
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int id = -1;
        if(db.isOpen()){
            ContentValues values = new ContentValues();
            values.put(InviteMessgeDao.COLUMN_NAME_FROM, message.getFrom());
            values.put(InviteMessgeDao.COLUMN_NAME_GROUP_ID, message.getGroupId());
            values.put(InviteMessgeDao.COLUMN_NAME_GROUP_Name, message.getGroupName());
            values.put(InviteMessgeDao.COLUMN_NAME_REASON, message.getReason());
            values.put(InviteMessgeDao.COLUMN_NAME_TIME, message.getTime());
            values.put(InviteMessgeDao.COLUMN_NAME_STATUS, message.getStatus().ordinal());
            values.put(InviteMessgeDao.COLUMN_NAME_GROUPINVITER, message.getGroupInviter());
            db.insert(InviteMessgeDao.TABLE_NAME, null, values);
            
            Cursor cursor = db.rawQuery("select last_insert_rowid() from " + InviteMessgeDao.TABLE_NAME,null); 
            if(cursor.moveToFirst()){
                id = cursor.getInt(0);
            }
            
            cursor.close();
        }
        return id;
    }
    
    /**
     * update message
     * @param msgId
     * @param values
     */
    synchronized public void updateMessage(int msgId,ContentValues values){
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        if(db.isOpen()){
            db.update(InviteMessgeDao.TABLE_NAME, values, InviteMessgeDao.COLUMN_NAME_ID + " = ?", new String[]{String.valueOf(msgId)});
        }
    }
    
    /**
     * get messges
     * @return
     */
    synchronized public List<InviteMessage> getMessagesList(){
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<InviteMessage> msgs = new ArrayList<InviteMessage>();
        if(db.isOpen()){
            Cursor cursor = db.rawQuery("select * from " + InviteMessgeDao.TABLE_NAME + " desc",null);
            while(cursor.moveToNext()){
                InviteMessage msg = new InviteMessage();
                int id = cursor.getInt(cursor.getColumnIndex(InviteMessgeDao.COLUMN_NAME_ID));
                String from = cursor.getString(cursor.getColumnIndex(InviteMessgeDao.COLUMN_NAME_FROM));
                String groupid = cursor.getString(cursor.getColumnIndex(InviteMessgeDao.COLUMN_NAME_GROUP_ID));
                String groupname = cursor.getString(cursor.getColumnIndex(InviteMessgeDao.COLUMN_NAME_GROUP_Name));
                String reason = cursor.getString(cursor.getColumnIndex(InviteMessgeDao.COLUMN_NAME_REASON));
                long time = cursor.getLong(cursor.getColumnIndex(InviteMessgeDao.COLUMN_NAME_TIME));
                int status = cursor.getInt(cursor.getColumnIndex(InviteMessgeDao.COLUMN_NAME_STATUS));
                String groupInviter = cursor.getString(cursor.getColumnIndex(InviteMessgeDao.COLUMN_NAME_GROUPINVITER));
                
                msg.setId(id);
                msg.setFrom(from);
                msg.setGroupId(groupid);
                msg.setGroupName(groupname);
                msg.setReason(reason);
                msg.setTime(time);
                msg.setGroupInviter(groupInviter);
                
                if(status == InviteMessage.InviteMesageStatus.BEINVITEED.ordinal())
                    msg.setStatus(InviteMessage.InviteMesageStatus.BEINVITEED);
                else if(status == InviteMessage.InviteMesageStatus.BEAGREED.ordinal())
                    msg.setStatus(InviteMessage.InviteMesageStatus.BEAGREED);
                else if(status == InviteMessage.InviteMesageStatus.BEREFUSED.ordinal())
                    msg.setStatus(InviteMessage.InviteMesageStatus.BEREFUSED);
                else if(status == InviteMessage.InviteMesageStatus.AGREED.ordinal())
                    msg.setStatus(InviteMessage.InviteMesageStatus.AGREED);
                else if(status == InviteMessage.InviteMesageStatus.REFUSED.ordinal())
                    msg.setStatus(InviteMessage.InviteMesageStatus.REFUSED);
                else if(status == InviteMessage.InviteMesageStatus.BEAPPLYED.ordinal())
                    msg.setStatus(InviteMessage.InviteMesageStatus.BEAPPLYED);
                else if(status == InviteMessage.InviteMesageStatus.GROUPINVITATION.ordinal())
                    msg.setStatus(InviteMessage.InviteMesageStatus.GROUPINVITATION);
                else if(status == InviteMessage.InviteMesageStatus.GROUPINVITATION_ACCEPTED.ordinal())
                    msg.setStatus(InviteMessage.InviteMesageStatus.GROUPINVITATION_ACCEPTED);
                else if(status == InviteMessage.InviteMesageStatus.GROUPINVITATION_DECLINED.ordinal())
                    msg.setStatus(InviteMessage.InviteMesageStatus.GROUPINVITATION_DECLINED);
                
                msgs.add(msg);
            }
            cursor.close();
        }
        return msgs;
    }
    
    /**
     * delete invitation message
     * @param from
     */
    synchronized public void deleteMessage(String from){
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        if(db.isOpen()){
            db.delete(InviteMessgeDao.TABLE_NAME, InviteMessgeDao.COLUMN_NAME_FROM + " = ?", new String[]{from});
        }
    }
    
    synchronized int getUnreadNotifyCount(){
        int count = 0;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        if(db.isOpen()){
            Cursor cursor = db.rawQuery("select " + InviteMessgeDao.COLUMN_NAME_UNREAD_MSG_COUNT + " from " + InviteMessgeDao.TABLE_NAME, null);
            if(cursor.moveToFirst()){
                count = cursor.getInt(0);
            }
            cursor.close();
        }
         return count;
    }
    
    synchronized void setUnreadNotifyCount(int count){
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        if(db.isOpen()){
            ContentValues values = new ContentValues();
            values.put(InviteMessgeDao.COLUMN_NAME_UNREAD_MSG_COUNT, count);

            db.update(InviteMessgeDao.TABLE_NAME, values, null,null);
        }
    }
    
    synchronized public void closeDB(){
        if(dbHelper != null){
            dbHelper.closeDB();
        }
        dbMgr = null;
    }
    
    
    /**
     * Save Robot list
     */
	synchronized public void saveRobotList(List<RobotUser> robotList) {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		if (db.isOpen()) {
			db.delete(UserDao.ROBOT_TABLE_NAME, null, null);
			for (RobotUser item : robotList) {
				ContentValues values = new ContentValues();
				values.put(UserDao.ROBOT_COLUMN_NAME_ID, item.getUsername());
				if (item.getNick() != null)
					values.put(UserDao.ROBOT_COLUMN_NAME_NICK, item.getNick());
				if (item.getAvatar() != null)
					values.put(UserDao.ROBOT_COLUMN_NAME_AVATAR, item.getAvatar());
				db.replace(UserDao.ROBOT_TABLE_NAME, null, values);
			}
		}
	}
    
    /**
     * load robot list
     */
	synchronized public Map<String, RobotUser> getRobotList() {
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		Map<String, RobotUser> users = null;
		if (db.isOpen()) {
			Cursor cursor = db.rawQuery("select * from " + UserDao.ROBOT_TABLE_NAME, null);
			if(cursor.getCount()>0){
				users = new Hashtable<String, RobotUser>();
			}
            while (cursor.moveToNext()) {
				String username = cursor.getString(cursor.getColumnIndex(UserDao.ROBOT_COLUMN_NAME_ID));
				String nick = cursor.getString(cursor.getColumnIndex(UserDao.ROBOT_COLUMN_NAME_NICK));
				String avatar = cursor.getString(cursor.getColumnIndex(UserDao.ROBOT_COLUMN_NAME_AVATAR));
				RobotUser user = new RobotUser(username);
				user.setNick(nick);
				user.setAvatar(avatar);
				String headerName = null;
				if (!TextUtils.isEmpty(user.getNick())) {
					headerName = user.getNick();
				} else {
					headerName = user.getUsername();
				}
				if(Character.isDigit(headerName.charAt(0))){
					user.setInitialLetter("#");
				}else{
					user.setInitialLetter(HanziToPinyin.getInstance().get(headerName.substring(0, 1)).get(0).target
							.substring(0, 1).toUpperCase());
					char header = user.getInitialLetter().toLowerCase().charAt(0);
					if (header < 'a' || header > 'z') {
						user.setInitialLetter("#");
					}
				}

                try {
                    users.put(username, user);
                } catch (Exception e) {
                    e.printStackTrace();
                }
			}
			cursor.close();
		}
		return users;
	}

    public synchronized boolean saveUser(User user){
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(UserDao.USER_COLUMN_NAME,user.getMUserName());
        values.put(UserDao.USER_COLUMN_NICk,user.getMUserNick());
        values.put(UserDao.USER_COLUMN_AVATAR_ID,user.getMAvatarId());
        values.put(UserDao.USER_COLUMN_AVATAR_TYPE,user.getMAvatarType());
        values.put(UserDao.USER_COLUMN_AVATA_PATH,user.getMAvatarPath());
        values.put(UserDao.USER_COLUMN_AVATAR_SUFFIX,user.getMAvatarSuffix());
        values.put(UserDao.USER_COLUMN_AVATAR_LASTAUPDATE_TIME,user.getMAvatarLastUpdateTime());
        if(db.isOpen()){
            return  db.replace(UserDao.USER_TABLE_NAME,null,values)!=-1;
        }
        return false;
    }

    public synchronized User getUset(String username){
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String sql = "select * from " + UserDao.USER_TABLE_NAME + " where "
                + UserDao.USER_COLUMN_NAME + " =? ";
        User user = null;
        Cursor cursor = db.rawQuery(sql,new String[]{username});
        while (cursor.moveToNext()){
            user = new User();
            user.setMUserName(cursor.getString(cursor.getColumnIndex(UserDao.USER_COLUMN_NAME)));
            user.setMUserNick(cursor.getString(cursor.getColumnIndex(UserDao.USER_COLUMN_NICk)));
            user.setMAvatarId(cursor.getInt(cursor.getColumnIndex(UserDao.USER_COLUMN_NICk)));
            user.setMAvatarType(cursor.getInt(cursor.getColumnIndex(UserDao.USER_COLUMN_NICk)));
            user.setMAvatarPath(cursor.getString(cursor.getColumnIndex(UserDao.USER_COLUMN_NICk)));
            user.setMAvatarSuffix(cursor.getString(cursor.getColumnIndex(UserDao.USER_COLUMN_NICk)));
            user.setMAvatarLastUpdateTime(cursor.getString(cursor.getColumnIndex(UserDao.USER_COLUMN_NICk)));
            return  user;
        }
        return user;
    }

    public synchronized boolean updateUser(User user){
        int result = -1;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String sql = UserDao.USER_TABLE_NAME + "=?";
        ContentValues values = new ContentValues();
        values.put(UserDao.USER_COLUMN_NICk,user.getMUserNick());
        if(db.isOpen()){
            result = db.update(UserDao.USER_TABLE_NAME,values,sql,new String[]{user.getMUserName()});
        }
        return result>0;
    }

    synchronized public Map<String, User> getAppContactList() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Map<String, User> users = new Hashtable<String, User>();
        if (db.isOpen()) {
            Cursor cursor = db.rawQuery("select * from " + UserDao.USER_TABLE_NAME /* + " desc" */, null);
            while (cursor.moveToNext()) {
                String username = cursor.getString(cursor.getColumnIndex(UserDao.USER_COLUMN_NAME));
                User user = new User(username);
                user.setMUserNick(cursor.getString(cursor.getColumnIndex(UserDao.USER_COLUMN_NICk)));
                user.setMAvatarId(cursor.getInt(cursor.getColumnIndex(UserDao.USER_COLUMN_NICk)));
                user.setMAvatarType(cursor.getInt(cursor.getColumnIndex(UserDao.USER_COLUMN_NICk)));
                user.setMAvatarPath(cursor.getString(cursor.getColumnIndex(UserDao.USER_COLUMN_NICk)));
                user.setMAvatarSuffix(cursor.getString(cursor.getColumnIndex(UserDao.USER_COLUMN_NICk)));
                user.setMAvatarLastUpdateTime(cursor.getString(cursor.getColumnIndex(UserDao.USER_COLUMN_NICk)));
                EaseCommonUtils.setAppUserInitialLetter(user);

                users.put(username, user);
            }
            cursor.close();
        }
        return users;
    }

    synchronized public void saveAppContactList(List<User> contactList) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        if (db.isOpen()) {
            db.delete(UserDao.USER_TABLE_NAME, null, null);
            for (User user : contactList) {
                ContentValues values = new ContentValues();
                values.put(UserDao.USER_COLUMN_NAME, user.getMUserName());
                if(user.getMUserNick() != null)
                    values.put(UserDao.USER_COLUMN_NICk,user.getMUserNick());
                if(user.getMAvatarId() != null)
                    values.put(UserDao.USER_COLUMN_AVATAR_ID,user.getMAvatarId());
                if(user.getMAvatarType() != null)
                    values.put(UserDao.USER_COLUMN_AVATAR_TYPE,user.getMAvatarType());
                if(user.getMAvatarPath() != null)
                    values.put(UserDao.USER_COLUMN_AVATA_PATH,user.getMAvatarPath());
                if(user.getMAvatarSuffix() != null)
                    values.put(UserDao.USER_COLUMN_AVATAR_SUFFIX,user.getMAvatarSuffix());
                if(user.getMAvatarLastUpdateTime() != null)
                    values.put(UserDao.USER_COLUMN_AVATAR_LASTAUPDATE_TIME,user.getMAvatarLastUpdateTime());
                db.replace(UserDao.USER_TABLE_NAME, null, values);
            }
        }
    }

    synchronized public void saveAppContact(User user){
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
            values.put(UserDao.USER_TABLE_NAME, user.getMUserName());
        if(user.getMUserNick() != null)
            values.put(UserDao.USER_COLUMN_NICk,user.getMUserNick());
        if(user.getMAvatarId() != null)
            values.put(UserDao.USER_COLUMN_AVATAR_ID,user.getMAvatarId());
        if(user.getMAvatarType() != null)
            values.put(UserDao.USER_COLUMN_AVATAR_TYPE,user.getMAvatarType());
        if(user.getMAvatarPath() != null)
            values.put(UserDao.USER_COLUMN_AVATA_PATH,user.getMAvatarPath());
        if(user.getMAvatarSuffix() != null)
            values.put(UserDao.USER_COLUMN_AVATAR_SUFFIX,user.getMAvatarSuffix());
        if(user.getMAvatarLastUpdateTime() != null)
            values.put(UserDao.USER_COLUMN_AVATAR_LASTAUPDATE_TIME,user.getMAvatarLastUpdateTime());
        if(db.isOpen()){
            db.replace(UserDao.USER_TABLE_NAME, null, values);
        }
    }
}

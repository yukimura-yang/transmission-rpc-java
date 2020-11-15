package com.costular.transmission_rc.api;

import com.costular.transmission_rc.TransmissionCallback;
import com.costular.transmission_rc.api.domain.*;
import com.costular.transmission_rc.api.domain.ids.Ids;
import com.costular.transmission_rc.http.*;
import com.costular.transmission_rc.http.command.general.CheckPortCommand;
import com.costular.transmission_rc.http.command.general.CloseConnectionCommand;
import com.costular.transmission_rc.http.command.general.GetSessionDataCommand;
import com.costular.transmission_rc.http.command.general.SetSessionDataCommand;
import com.costular.transmission_rc.http.command.statistics.SessionStatsBody;
import com.costular.transmission_rc.http.command.torrent.*;
import com.costular.transmission_rc.utils.TorrentUtils;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import retrofit2.Response;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class TransmissionClient {

    private static TransmissionClient instance = null;

    /**
     * Returns a TransmissionClient instance with URL
     *
     * @param url The URL of Transmission RPC
     * @return TransmissionClient instance
     */
    public static TransmissionClient getInstance(String url) {
        if (instance == null) {
            instance = new TransmissionClient(url, null, null);
        }
        return instance;
    }

    public static TransmissionClient getInstance(String url, String account, String password) {
        if (instance == null) {
            instance = new TransmissionClient(url, account, password);
        }
        return instance;
    }

    /**
     * Returns a TransmissionClient instance with host and port
     *
     * @param host The transmission rpc host (eg: localhost or 192.168.1.100)
     * @param port The transmission prc port (eg: 9091)
     * @return TransmissionClient instance
     * @deprecated
     */
    public static TransmissionClient getInstance(String host, int port) {
        return getInstance(String.format("http://%d:%s", host, port));
    }

    private String url;
    private String csrf;
    private String authorization;

    private TransmissionClient(String url, String account, String password) {
        if (url != null) {
            if (url.endsWith("/")) {
                throw new IllegalArgumentException("URL must ends without a slash (/)");
            }
        }
        if (account != null && password != null) {
            String content = account + ":" + password;
            content = Base64.encode(content.getBytes());
            authorization = "Basic " + content;
        }

        this.url = url;
        loadCSRF();
    }

    private void loadCSRF() {
        try {
            Response response = APIFactory.createService(url, null, authorization, SessionService.class)
                    .getSessionStatistics(new SessionStatsBody())
                    .execute();
            csrf = response.headers().get("X-Transmission-Session-Id");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void checkPort(TransmissionCallback<Boolean> callback) {
        APIFactory.createService(url, csrf, authorization, GeneralService.class)
                .checkPort(new CheckPortCommand())
                .enqueue(new APIResponse<>(callback));
    }

    public void getSessionData(TransmissionCallback<SessionInfo> callback) {
        APIFactory.createService(url, csrf, authorization, GeneralService.class)
                .getSessionInfo(new GetSessionDataCommand())
                .enqueue(new APIResponse<>(callback));
    }

    public void changeSessionData(SetSessionData changeData, TransmissionCallback<Boolean> callback) {
        APIFactory.createService(url, csrf, authorization, GeneralService.class)
                .setSessionInfo(new SetSessionDataCommand(changeData))
                .enqueue(new APIResponse<>(callback));
    }

    /**
     * Get info of torrent
     *
     * @param id       torrent id
     * @param callback callback result
     */
    public void getTorrentInfo(long id, TransmissionCallback<TorrentInfoCollection> callback) {
        GetTorrentCommand command = new GetTorrentCommand(new GetTorrentArguments(Ids.createId(id)));

        APIFactory.createService(url, csrf, authorization, TorrentService.class)
                .getTorrentInfo(command)
                .enqueue(new APIResponse<>(callback));
    }

    /**
     * Get info of all torrents
     *
     * @param callback callback result
     */
    public void getAllTorrentsInfo(TransmissionCallback<TorrentInfoCollection> callback) {
        GetTorrentCommand command = new GetTorrentCommand(new GetTorrentArguments(Ids.createAllIds()));

        APIFactory.createService(url, csrf, authorization, TorrentService.class)
                .getTorrentInfo(command)
                .enqueue(new APIResponse<>(callback));
    }

    public void getSessionStatistics(TransmissionCallback<SessionStats> callback) {
        APIFactory.createService(url, csrf, authorization, SessionService.class)
                .getSessionStatistics(new SessionStatsBody())
                .enqueue(new APIResponse<>(callback));
    }

    /**
     * Add torrent
     *
     * @param link     magnet link
     * @param paused   torrent should be paused
     * @param callback callback result
     */
    public void addTorrent(String link, boolean paused, TransmissionCallback<AddedTorrentInfo> callback) {
        AddTorrentArguments arguments = new AddTorrentArguments();
        arguments.setFilename(link);
        arguments.setPaused(paused);

        APIFactory.createService(url, csrf, authorization, TorrentService.class)
                .addTorrent(new AddTorrentCommand(arguments))
                .enqueue(new APIResponse<>(callback));
    }

    public void addTorrent(File file, boolean paused, TransmissionCallback<AddedTorrentInfo> callback) {
        String encodedBase64Meta = TorrentUtils.getTorrentB64Encoded(file);

        AddTorrentArguments arguments = new AddTorrentArguments();
        arguments.setMetainfo(encodedBase64Meta);
        arguments.setPaused(paused);

        APIFactory.createService(url, csrf, authorization, TorrentService.class)
                .addTorrent(new AddTorrentCommand(arguments))
                .enqueue(new APIResponse<>(callback));
    }

    public void addTorrent(String link, TransmissionCallback<AddedTorrentInfo> callback) {
        addTorrent(link, false, callback);
    }

    public void addTorrent(File file, TransmissionCallback<AddedTorrentInfo> callback) {
        addTorrent(file, false, callback);
    }

    private void moveTorrent(Ids ids, String newPath, boolean move, TransmissionCallback<Boolean> callback) {
        MoveTorrentCommand command = new MoveTorrentCommand(new MoveTorrentArguments(ids, newPath, move));

        APIFactory.createService(url, csrf, authorization, TorrentService.class)
                .moveTorrent(command)
                .enqueue(new APIResponse<>(callback));
    }

    public void moveTorrent(long id, String newPath, TransmissionCallback<Boolean> callback) {
        moveTorrent(Ids.createId(id), newPath, false, callback);
    }

    public void moveTorrent(long id, String newPath, boolean move, TransmissionCallback<Boolean> callback) {
        moveTorrent(Ids.createId(id), newPath, move, callback);
    }

    public void moveTorrents(List<Long> ids, String newPath, boolean move, TransmissionCallback<Boolean> callback) {
        moveTorrent(Ids.createIds(ids), newPath, move, callback);
    }

    public void moveTorrents(List<Long> ids, String newPath, TransmissionCallback<Boolean> callback) {
        moveTorrent(Ids.createIds(ids), newPath, false, callback);
    }

    private void deleteTorrent(Ids ids, boolean deleteLocalData, TransmissionCallback<Boolean> callback) {
        RemoveTorrentArguments arguments = new RemoveTorrentArguments(ids, deleteLocalData);

        APIFactory.createService(url, csrf, authorization, TorrentService.class)
                .removeTorrent(new RemoveTorrentCommand(arguments))
                .enqueue(new APIResponse<>(callback));
    }

    /**
     * Delete all torrents
     *
     * @param deleteLocalData delete local data
     * @param callback        callback result
     */
    public void deleteAllTorrents(boolean deleteLocalData, TransmissionCallback<Boolean> callback) {
        deleteTorrent(Ids.createAllIds(), deleteLocalData, callback);
    }

    /**
     * Delete torrents by id's list
     *
     * @param torrentIds      List of id's of the torrents
     * @param deleteLocalData delete local data
     * @param callback        callback result
     */
    public void deleteTorrents(List<Long> torrentIds, boolean deleteLocalData, TransmissionCallback<Boolean> callback) {
        Ids ids = Ids.createIds(torrentIds);
        deleteTorrent(ids, deleteLocalData, callback);
    }

    /**
     * Delete single torrent by its id
     *
     * @param torrentId       id of the torrent
     * @param deleteLocalData delete local data
     * @param callback        callback result
     */
    public void deleteTorrent(long torrentId, boolean deleteLocalData, TransmissionCallback<Boolean> callback) {
        Ids ids = Ids.createId(torrentId);
        deleteTorrent(ids, deleteLocalData, callback);
    }


    private void torrentAction(Ids ids, TorrentAction action, TransmissionCallback<Boolean> callback) {
        TorrentActionCommand body = TorrentActionCommand.create(action, ids);

        APIFactory.createService(url, csrf, authorization, TorrentService.class)
                .torrentAction(body)
                .enqueue(new APIResponse<>(callback));
    }

    /**
     * Do action for single torrent
     *
     * @param torrentId
     * @param torrentAction
     * @param callback
     */
    public void doAction(long torrentId, TorrentAction torrentAction, TransmissionCallback<Boolean> callback) {
        torrentAction(Ids.createId(torrentId), torrentAction, callback);
    }

    /**
     * Do action for many torrents
     *
     * @param torrents
     * @param torrentAction
     * @param callback
     */
    public void doActionTorrents(List<Long> torrents, TorrentAction torrentAction, TransmissionCallback<Boolean> callback) {
        torrentAction(Ids.createIds(torrents), torrentAction, callback);
    }

    /**
     * Do action for all torrents
     *
     * @param torrentAction
     * @param callback
     */
    public void doActionToAll(TorrentAction torrentAction, TransmissionCallback<Boolean> callback) {
        torrentAction(Ids.createAllIds(), torrentAction, callback);
    }

    private void setTorrentInfo(Ids ids, SetTorrentArguments torrentInfo, TransmissionCallback<Boolean> callback) {
        torrentInfo.setIds(ids);
        SetTorrentCommand setTorrentCommand = new SetTorrentCommand(torrentInfo);

        APIFactory.createService(url, csrf, authorization, TorrentService.class)
                .setTorrent(setTorrentCommand)
                .enqueue(new APIResponse<>(callback));
    }

    public void setAllTorrentsInfo(SetTorrentArguments torrentInfo, TransmissionCallback<Boolean> callback) {
        setTorrentInfo(null, torrentInfo, callback);
    }

    public void setTorrentInfo(long id, SetTorrentArguments torrentInfo, TransmissionCallback<Boolean> callback) {
        setTorrentInfo(Ids.createId(id), torrentInfo, callback);
    }

    private void moveQueue(Ids ids, QueueAction queueAction, TransmissionCallback<Boolean> callback) {
        QueueMovementCommand command = QueueMovementCommand.create(queueAction, ids);

        APIFactory.createService(url, csrf, authorization, TorrentService.class)
                .queueMovement(command)
                .enqueue(new APIResponse<>(callback));
    }

    public void moveTorrent(long id, QueueAction action, TransmissionCallback<Boolean> callback) {
        moveQueue(Ids.createId(id), action, callback);
    }

    public void moveTorrents(List<Long> ids, QueueAction action, TransmissionCallback<Boolean> callback) {
        moveQueue(Ids.createIds(ids), action, callback);
    }

    public void closeSession() {
        APIFactory.createService(url, csrf, authorization, GeneralService.class)
                .closeConnection(new CloseConnectionCommand())
                .enqueue(null);
    }
}

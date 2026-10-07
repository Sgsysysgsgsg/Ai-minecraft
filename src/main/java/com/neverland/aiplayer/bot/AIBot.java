package com.neverland.aiplayer.bot;

import io.netty.channel.Channel;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.geysermc.mcprotocollib.network.ClientSession;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.tcp.TcpClientSession;
import org.geysermc.mcprotocollib.protocol.MinecraftProtocol;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundContainerSetContentPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundContainerSetSlotPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundOpenScreenPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundContainerClosePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerPositionPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatCommandPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClickPacket;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ClickItemAction;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerActionType;
import org.geysermc.mcprotocollib.protocol.data.game.item.HashedStack;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AIBot {
    private final JavaPlugin plugin;
    private final FileConfiguration cfg;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Session session;
    private int containerId = -1, stateId = 0;
    private String guiTitle = "";
    private long lastThink;

    public AIBot(JavaPlugin plugin, FileConfiguration cfg) { this.plugin=plugin; this.cfg=cfg; }

    public boolean isRunning(){ return running.get(); }

    public void start() {
        if (!running.compareAndSet(false,true)) return;
        String name=cfg.getString("bot.name","NeverBot");
        String host=cfg.getString("bot.host","127.0.0.1");
        int port=cfg.getInt("bot.port",0);
        if(port<=0) port=25565;
        MinecraftProtocol protocol=new MinecraftProtocol(name);
        session=new TcpClientSession(host,port,protocol);
        session.addListener(new SessionAdapter());
        session.connect();
        plugin.getLogger().info("Connecting AI player "+name+" to "+host+":"+port);
    }

    public void stop() {
        running.set(false);
        if(session!=null) session.disconnect("AIPlayer stopped");
        session=null;
    }

    public void tick() {
        if(!running.get() || session==null || !session.isConnected()) return;
        long now=System.currentTimeMillis();
        if(now-lastThink < cfg.getLong("bot.think-interval-ms",3000)) return;
        lastThink=now;
        World w=Bukkit.getWorld(cfg.getString("bot.spawn-world","spawn"));
        if(w==null) return;
        // The real player is controlled by packets; Bukkit is used as authoritative server-world observation.
        var p=Bukkit.getPlayerExact(cfg.getString("bot.name","NeverBot"));
        if(p==null) return;
        Location l=p.getLocation();
        if(l.getWorld()!=null) plugin.getLogger().fine("AI at "+l.getBlockX()+","+l.getBlockY()+","+l.getBlockZ());
    }

    private void sendCommand(String raw) {
        if(raw==null) return;
        String cmd=raw.startsWith("/")?raw.substring(1):raw;
        String root=cmd.split("\s+",2)[0].toLowerCase();
        var allowed=cfg.getStringList("bot.commands.allowed");
        if(!allowed.contains(root)) { plugin.getLogger().warning("Blocked learned command: "+root); return; }
        session.send(new ServerboundChatCommandPacket(cmd));
    }

    private final class SessionAdapter implements Session.Listener {
        @Override public void packetReceived(Session session, org.geysermc.mcprotocollib.network.packet.Packet packet) {
            if(packet instanceof ClientboundOpenScreenPacket p) {
                containerId=p.getContainerId(); stateId=0; guiTitle=p.getTitle().toString();
                plugin.getLogger().info("AI GUI opened: "+guiTitle+" (#"+containerId+")");
                if(guiTitle.toLowerCase().contains("survival")) plugin.getLogger().info("AI detected Survival GUI.");
            } else if(packet instanceof ClientboundContainerSetContentPacket p) {
                stateId=p.getStateId();
                plugin.getLogger().fine("AI received "+p.getItems().length+" GUI slots.");
            } else if(packet instanceof ClientboundContainerSetSlotPacket p) {
                stateId=p.getStateId();
            } else if(packet instanceof ClientboundContainerClosePacket) {
                containerId=-1; guiTitle="";
            }
        }
        @Override public void connected(Session session) { plugin.getLogger().info("AI player connected."); }
        @Override public void disconnected(Session session, org.geysermc.mcprotocollib.network.DisconnectReason reason, String msg) {
            running.set(false); plugin.getLogger().warning("AI disconnected: "+msg);
        }
    }

    // Reserved for the next GUI-learning phase: click is sent only with a server-provided container/state.
    public void clickSlot(int slot) {
        if(session==null || !session.isConnected() || containerId<0) return;
        session.send(new ServerboundContainerClickPacket(containerId,stateId,slot,
            ContainerActionType.CLICK_ITEM,ClickItemAction.LEFT_CLICK,null,Map.<Integer,HashedStack>of()));
    }
}
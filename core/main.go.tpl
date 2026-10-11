package main

import (
	"context"
	"flag"
	"log"
	"net"
	"os"
	"os/signal"
	"syscall"
	"time"

	"__MOD__/pkg/clientcore"
)

func main() {
	listen := flag.String("listen", "127.0.0.1:9000", "")
	peer := flag.String("peer", "", "")
	vk := flag.String("vk-link", "", "")
	n := flag.Int("n", 8, "")
	udp := flag.Bool("udp", false, "")
	flag.Parse()

	// На Android pure-Go не видит resolv.conf, поэтому DNS задаём вручную (трафик приложения идёт мимо VPN)
	net.DefaultResolver = &net.Resolver{PreferGo: true, Dial: func(ctx context.Context, _, _ string) (net.Conn, error) {
		d := net.Dialer{Timeout: 3 * time.Second}
		if c, err := d.DialContext(ctx, "udp", "8.8.8.8:53"); err == nil {
			return c, nil
		}
		return d.DialContext(ctx, "udp", "77.88.8.8:53")
	}}
	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()
	err := clientcore.Run(ctx, clientcore.Config{
		Listen: *listen, PeerAddr: *peer, VKLink: *vk,
		NumStreams: *n, UseUDP: *udp, ManualCaptcha: true,
	})
	log.Fatalf("core stopped: %v", err)
}

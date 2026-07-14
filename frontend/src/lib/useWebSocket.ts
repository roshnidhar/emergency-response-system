import { useEffect, useRef, useState, useCallback } from 'react'
import type { WSMessage } from '../types'

// In dev: Vite proxies /api/ws → ws://localhost:8080/api/ws
// In prod docker: connects directly
const WS_URL = `ws://${window.location.hostname}:8080/api/ws/emergency`

export function useEmergencyWS(onMessage: (msg: WSMessage) => void) {
  const wsRef = useRef<WebSocket | null>(null)
  const [connected, setConnected] = useState(false)
  const timer = useRef<ReturnType<typeof setTimeout>>()
  const onMessageRef = useRef(onMessage)
  onMessageRef.current = onMessage

  const connect = useCallback(() => {
    const ws = new WebSocket(WS_URL)
    wsRef.current = ws

    ws.onopen = () => setConnected(true)

    ws.onmessage = (e) => {
      try {
        onMessageRef.current(JSON.parse(e.data) as WSMessage)
      } catch { /* ignore bad frames */ }
    }

    ws.onclose = () => {
      setConnected(false)
      timer.current = setTimeout(connect, 3000)  // auto-reconnect
    }

    ws.onerror = () => ws.close()
  }, [])

  useEffect(() => {
    connect()
    return () => {
      clearTimeout(timer.current)
      wsRef.current?.close()
    }
  }, [connect])

  return { connected }
}

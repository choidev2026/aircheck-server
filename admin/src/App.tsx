/**
 * Admin 관리 페이지
 * 
 * React 기본 개념:
 * - useState: 상태(데이터) 관리. 값이 바뀌면 화면 자동 업데이트
 * - async/await: 비동기 API 호출
 * - JSX: HTML처럼 생긴 문법으로 UI 작성
 */

import { useState } from 'react'

// ─── 타입 정의 ────────────────────────────────────────────────────────────────
// TypeScript: 데이터의 형태를 미리 정의 (Java의 class/interface와 비슷)

interface VersionInfo {
  latestVersion: string      // 최신 버전
  minimumVersion: string     // 최소 필수 버전
  forceUpdate: boolean       // 강제 업데이트 여부
  updateUrl: string          // 플레이스토어 URL
}

// ─── 메인 컴포넌트 ─────────────────────────────────────────────────────────────

function App() {
  // useState: 상태 변수 선언
  // [값, 값을변경하는함수] = useState(초기값)
  const [apiKey, setApiKey] = useState('')           // 입력한 API 키
  const [isAuthenticated, setIsAuthenticated] = useState(false)  // 인증 여부
  const [versionInfo, setVersionInfo] = useState<VersionInfo | null>(null)
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState('')         // 알림 메시지

  // ─── API 호출 함수들 ──────────────────────────────────────────────────────────

  // 버전 정보 불러오기
  const fetchVersionInfo = async () => {
    setLoading(true)
    try {
      const response = await fetch('/api/admin/version', {
        headers: { 'X-Admin-Api-Key': apiKey }
      })
      if (response.ok) {
        const data = await response.json()
        setVersionInfo(data)
        setIsAuthenticated(true)
      } else {
        setMessage('❌ 인증 실패: API 키를 확인하세요')
        setIsAuthenticated(false)
      }
    } catch (error) {
      setMessage('❌ 서버 연결 실패')
    }
    setLoading(false)
  }

  // 버전 정보 저장하기
  const saveVersionInfo = async () => {
    if (!versionInfo) return
    
    setLoading(true)
    try {
      const response = await fetch('/api/admin/version', {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'X-Admin-Api-Key': apiKey
        },
        body: JSON.stringify(versionInfo)
      })
      if (response.ok) {
        setMessage('✅ 저장 완료!')
      } else {
        setMessage('❌ 저장 실패')
      }
    } catch (error) {
      setMessage('❌ 서버 연결 실패')
    }
    setLoading(false)
    
    // 3초 후 메시지 지우기
    setTimeout(() => setMessage(''), 3000)
  }

  // ─── 화면 렌더링 ──────────────────────────────────────────────────────────────
  // JSX: HTML처럼 생긴 문법. JavaScript 안에서 UI를 작성
  // className: HTML의 class 대신 사용 (class는 JS 예약어)
  // onClick, onChange: 이벤트 핸들러

  return (
    <div className="min-h-screen bg-gray-100">
      {/* 헤더 */}
      <header className="bg-blue-600 text-white p-4 shadow-lg">
        <h1 className="text-2xl font-bold">🔧 Aircheck Admin</h1>
      </header>

      <main className="max-w-2xl mx-auto p-6">
        {/* 로그인 전: API 키 입력 */}
        {!isAuthenticated ? (
          <div className="bg-white rounded-lg shadow p-6">
            <h2 className="text-xl font-semibold mb-4">🔐 관리자 인증</h2>
            <input
              type="password"
              placeholder="Admin API Key"
              value={apiKey}
              onChange={(e) => setApiKey(e.target.value)}
              className="w-full p-3 border rounded mb-4"
            />
            <button
              onClick={fetchVersionInfo}
              disabled={loading || !apiKey}
              className="w-full bg-blue-600 text-white p-3 rounded hover:bg-blue-700 disabled:bg-gray-400"
            >
              {loading ? '확인 중...' : '로그인'}
            </button>
            {message && <p className="mt-4 text-center">{message}</p>}
          </div>
        ) : (
          /* 로그인 후: 버전 관리 */
          <div className="space-y-6">
            {/* 버전 정보 카드 */}
            <div className="bg-white rounded-lg shadow p-6">
              <h2 className="text-xl font-semibold mb-4">📱 앱 버전 관리</h2>
              
              {versionInfo && (
                <div className="space-y-4">
                  {/* 최신 버전 */}
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      최신 버전
                    </label>
                    <input
                      type="text"
                      value={versionInfo.latestVersion}
                      onChange={(e) => setVersionInfo({
                        ...versionInfo,  // 기존 값 유지 (spread 연산자)
                        latestVersion: e.target.value
                      })}
                      className="w-full p-2 border rounded"
                      placeholder="1.0.0"
                    />
                  </div>

                  {/* 최소 필수 버전 */}
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      최소 필수 버전
                    </label>
                    <input
                      type="text"
                      value={versionInfo.minimumVersion}
                      onChange={(e) => setVersionInfo({
                        ...versionInfo,
                        minimumVersion: e.target.value
                      })}
                      className="w-full p-2 border rounded"
                      placeholder="1.0.0"
                    />
                  </div>

                  {/* 강제 업데이트 토글 */}
                  <div className="flex items-center justify-between">
                    <span className="text-sm font-medium text-gray-700">
                      강제 업데이트
                    </span>
                    <button
                      onClick={() => setVersionInfo({
                        ...versionInfo,
                        forceUpdate: !versionInfo.forceUpdate
                      })}
                      className={`relative w-14 h-7 rounded-full transition-colors ${
                        versionInfo.forceUpdate ? 'bg-blue-600' : 'bg-gray-300'
                      }`}
                    >
                      <span className={`absolute top-1 w-5 h-5 bg-white rounded-full transition-transform ${
                        versionInfo.forceUpdate ? 'translate-x-8' : 'translate-x-1'
                      }`} />
                    </button>
                  </div>

                  {/* 플레이스토어 URL */}
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      스토어 URL
                    </label>
                    <input
                      type="text"
                      value={versionInfo.updateUrl}
                      onChange={(e) => setVersionInfo({
                        ...versionInfo,
                        updateUrl: e.target.value
                      })}
                      className="w-full p-2 border rounded"
                      placeholder="https://play.google.com/store/apps/details?id=..."
                    />
                  </div>

                  {/* 저장 버튼 */}
                  <button
                    onClick={saveVersionInfo}
                    disabled={loading}
                    className="w-full bg-green-600 text-white p-3 rounded hover:bg-green-700 disabled:bg-gray-400 font-semibold"
                  >
                    {loading ? '저장 중...' : '💾 저장'}
                  </button>

                  {message && (
                    <p className="text-center font-medium">{message}</p>
                  )}
                </div>
              )}
            </div>

            {/* 로그아웃 */}
            <button
              onClick={() => {
                setIsAuthenticated(false)
                setVersionInfo(null)
                setApiKey('')
              }}
              className="w-full bg-gray-500 text-white p-2 rounded hover:bg-gray-600"
            >
              🚪 로그아웃
            </button>
          </div>
        )}
      </main>
    </div>
  )
}

export default App

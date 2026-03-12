import { Routes, Route } from 'react-router-dom';

export default function App() {
  return (
    <div className="min-h-screen flex flex-col bg-bg-light">
      <main className="flex-1 flex items-center justify-center">
        <Routes>
          <Route path="/" element={
            <div className="text-center">
              <h1 className="text-5xl font-extrabold text-accent-orange mb-4">TURBO</h1>
              <p className="text-text-gray text-lg">Peer-to-peer car rental platform</p>
            </div>
          } />
        </Routes>
      </main>
    </div>
  );
}

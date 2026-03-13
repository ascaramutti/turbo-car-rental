export default function Footer() {
  return (
    <footer className="bg-white border-t border-border py-6 mt-auto">
      <div className="max-w-7xl mx-auto px-4 text-center">
        <p className="text-sm text-text-gray">
          &copy; {new Date().getFullYear()} TURBO &mdash; Peer-to-peer car rental platform
        </p>
      </div>
    </footer>
  );
}

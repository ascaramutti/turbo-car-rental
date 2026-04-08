import { useNavigate } from 'react-router-dom';
import { Car, Shield, CreditCard, Star, Clock, Users } from 'lucide-react';

const FEATURES = [
  { icon: Car, title: 'Wide Selection', description: 'Browse hundreds of vehicles from trusted owners in your area' },
  { icon: Shield, title: 'Fully Insured', description: 'Every rental includes comprehensive insurance coverage' },
  { icon: CreditCard, title: 'Easy Payments', description: 'Secure transactions with transparent pricing, no hidden fees' },
  { icon: Star, title: 'Rated & Reviewed', description: 'Community ratings help you pick the perfect car and owner' },
  { icon: Clock, title: 'Flexible Rentals', description: 'Rent by the day or week with easy pickup and return' },
  { icon: Users, title: 'Trusted Community', description: 'Verified drivers and owners for a safe rental experience' },
];

export default function HomePage() {
  const navigate = useNavigate();

  return (
    <div>
      {/* Hero Section */}
      <section className="bg-bg-light py-16 sm:py-24">
        <div className="max-w-4xl mx-auto px-4 text-center">
          <h1 className="text-4xl sm:text-5xl font-extrabold text-accent-orange mb-4">
            WELCOME TO TURBO
          </h1>
          <p className="text-text-gray text-lg mb-10">
            Rent cars from real people. List your car and earn money.
          </p>

          <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
            <button
              onClick={() => navigate('/signup', { state: { role: 'DRIVER' } })}
              className="w-56 py-4 bg-driver-blue text-white font-bold text-lg rounded-xl hover:bg-driver-blue-light transition-colors shadow-md"
            >
              DRIVER
            </button>
            <button
              onClick={() => navigate('/signup', { state: { role: 'CAR_OWNER' } })}
              className="w-56 py-4 bg-owner-red text-white font-bold text-lg rounded-xl hover:bg-owner-red-light transition-colors shadow-md"
            >
              CAR OWNER
            </button>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section className="py-16 sm:py-20">
        <div className="max-w-6xl mx-auto px-4">
          <h2 className="text-2xl sm:text-3xl font-bold text-text-dark text-center mb-12">
            TURBO FEATURES
          </h2>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
            {FEATURES.map((feature) => (
              <div
                key={feature.title}
                className="bg-white border-2 border-accent-orange/30 rounded-2xl p-6 hover:border-accent-orange hover:shadow-md transition-all"
              >
                <div className="bg-accent-orange/10 w-12 h-12 rounded-xl flex items-center justify-center mb-4">
                  <feature.icon size={24} className="text-accent-orange" />
                </div>
                <h3 className="font-bold text-text-dark mb-2">{feature.title}</h3>
                <p className="text-sm text-text-gray">{feature.description}</p>
              </div>
            ))}
          </div>
        </div>
      </section>
    </div>
  );
}

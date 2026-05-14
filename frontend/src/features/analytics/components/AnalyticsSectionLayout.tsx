export const SectionHeader = ({
  icon,
  title,
  subtitle,
}: {
  icon: React.ReactNode;
  title: string;
  subtitle?: string;
}) => (
  <div className="section-header" style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '1rem' }}>
    <span style={{ color: 'var(--color-primary, #6366f1)' }}>{icon}</span>
    <div>
      <h2 style={{ margin: 0, fontSize: '1.1rem', fontWeight: 700 }}>{title}</h2>
      {subtitle && <p style={{ margin: 0, fontSize: '0.8rem', color: '#6b7280' }}>{subtitle}</p>}
    </div>
  </div>
);

export const TableCard = ({
  title,
  children,
  subTitle,
}: {
  title: string;
  children: React.ReactNode;
  subTitle?: string;
}) => (
  <div className={`dashboard-card ${subTitle ? 'zombies-card' : ''}`}>
    <h3 className="card-title">{title}</h3>
    {subTitle && (
      <p className="text-sm text-gray-500" style={{ marginTop: '-1rem', marginBottom: '1rem' }}>
        {subTitle}
      </p>
    )}
    {children}
  </div>
);

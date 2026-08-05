import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Line,
  LineChart,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis
} from 'recharts';
import type { CategoryAggregate, DecimalValue, TrendPoint } from '../api/types';
import { formatMoney } from '../lib/format';

const categoryColors = [
  '#0f766e',
  '#2563eb',
  '#d97706',
  '#db2777',
  '#7c3aed',
  '#0891b2',
  '#65a30d'
];

function amount(value: DecimalValue) {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : 0;
}

export function SpendingTrendChart({ data, currency }: { data: TrendPoint[]; currency: string }) {
  const chartData = data.map((point) => ({
    label: point.date.slice(5),
    amount: amount(point.amount)
  }));
  return (
    <div className="h-72 min-w-0" aria-label="Daily spending trend chart">
      <ResponsiveContainer width="100%" height="100%">
        <LineChart data={chartData} margin={{ top: 10, right: 10, left: -18, bottom: 0 }}>
          <CartesianGrid
            strokeDasharray="3 3"
            vertical={false}
            stroke="currentColor"
            opacity={0.12}
          />
          <XAxis dataKey="label" tickLine={false} axisLine={false} fontSize={12} />
          <YAxis tickLine={false} axisLine={false} fontSize={12} />
          <Tooltip formatter={(value) => formatMoney(Number(value), currency)} />
          <Line
            type="monotone"
            dataKey="amount"
            name="Spending"
            stroke="#0f766e"
            strokeWidth={3}
            dot={false}
            activeDot={{ r: 5 }}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}

export function CategoryDonutChart({
  data,
  currency
}: {
  data: CategoryAggregate[];
  currency: string;
}) {
  const chartData = data.map((item) => ({ name: item.categoryName, value: amount(item.amount) }));
  return (
    <div className="h-72 min-w-0" aria-label="Expense category breakdown chart">
      <ResponsiveContainer width="100%" height="100%">
        <PieChart>
          <Pie
            data={chartData}
            dataKey="value"
            nameKey="name"
            innerRadius="52%"
            outerRadius="78%"
            paddingAngle={2}
          >
            {chartData.map((item, index) => (
              <Cell key={item.name} fill={categoryColors[index % categoryColors.length]} />
            ))}
          </Pie>
          <Tooltip formatter={(value) => formatMoney(Number(value), currency)} />
          <Legend iconType="circle" iconSize={8} wrapperStyle={{ fontSize: 12 }} />
        </PieChart>
      </ResponsiveContainer>
    </div>
  );
}

export function IncomeExpenseChart({
  income,
  expense,
  currency
}: {
  income: DecimalValue;
  expense: DecimalValue;
  currency: string;
}) {
  const chartData = [
    { label: 'Selected month', Income: amount(income), Expenses: amount(expense) }
  ];
  return (
    <div className="h-72 min-w-0" aria-label="Income and expense comparison chart">
      <ResponsiveContainer width="100%" height="100%">
        <BarChart data={chartData} margin={{ top: 10, right: 10, left: -18, bottom: 0 }}>
          <CartesianGrid
            strokeDasharray="3 3"
            vertical={false}
            stroke="currentColor"
            opacity={0.12}
          />
          <XAxis dataKey="label" tickLine={false} axisLine={false} fontSize={12} />
          <YAxis tickLine={false} axisLine={false} fontSize={12} />
          <Tooltip formatter={(value) => formatMoney(Number(value), currency)} />
          <Legend />
          <Bar dataKey="Income" fill="#059669" radius={[8, 8, 0, 0]} />
          <Bar dataKey="Expenses" fill="#e11d48" radius={[8, 8, 0, 0]} />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}

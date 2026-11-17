import { lazy, Suspense, type ReactNode } from "react";
import { Link } from "react-router-dom";

const Silk = lazy(() => import("@/components/ui/silk"));

type Props = {
  destaque: string;
  apoio: string;
  children: ReactNode;
};

export function AuthShell({ destaque, apoio, children }: Props) {
  return (
    <div className="grid min-h-screen md:grid-cols-2">
      <div className="relative hidden overflow-hidden bg-[#08293d] md:flex">
        <div aria-hidden className="pointer-events-none absolute inset-0 h-full w-full animate-in fade-in duration-500">
          <Suspense fallback={null}>
            <Silk color="#0b3450" speed={5} scale={1.3} noiseIntensity={1.1} rotation={0.15} />
          </Suspense>
        </div>
        <div className="relative flex h-full w-full flex-col p-12">
          <Link to="/" className="w-fit text-lg font-bold tracking-tight text-white">
            Lexo
          </Link>
          <div className="flex flex-1 flex-col justify-center">
            <p className="max-w-sm text-3xl leading-snug font-medium text-white">{destaque}</p>
            <p className="mt-4 max-w-sm text-sm text-white/60">{apoio}</p>
          </div>
          <p className="text-xs text-white/40">© {new Date().getFullYear()} Lexo</p>
        </div>
      </div>

      <div className="flex flex-col justify-center px-6 py-16 md:px-16">
        <div className="mx-auto w-full max-w-sm">
          <Link to="/" className="mb-10 inline-flex text-lg font-bold tracking-tight md:hidden">
            Lexo
          </Link>
          {children}
        </div>
      </div>
    </div>
  );
}
